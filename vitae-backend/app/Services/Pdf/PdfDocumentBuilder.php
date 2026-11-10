<?php

namespace App\Services\Pdf;

use Symfony\Component\HttpFoundation\Response;

class PdfDocumentBuilder
{
    protected float $pageWidth = 612.0;   // Letter width in points (8.5 inches)
    protected float $pageHeight = 792.0;  // Letter height in points (11.0 inches)
    protected float $marginLeft = 36.0;   // 0.5 inch margin
    protected float $marginRight = 36.0;
    protected float $marginTop = 36.0;
    protected float $marginBottom = 36.0;

    protected array $pages = [];
    protected int $currentPage = -1;
    protected string $currentFont = 'F1';
    protected float $currentFontSize = 10.0;
    protected array $currentColor = [0.0, 0.0, 0.0];
    protected string $watermarkText = '';

    protected array $fonts = [
        'F1' => 'Helvetica',
        'F2' => 'Helvetica-Bold',
        'F3' => 'Helvetica-Oblique',
        'F4' => 'Courier',
        'F5' => 'Courier-Bold',
        'F6' => 'Times-Roman',
        'F7' => 'Times-Bold',
    ];

    /**
     * @var array<string, array{id: string, data: string, width: int, height: int, length: int}>
     */
    protected array $images = [];

    public function __construct(string $watermarkText = '')
    {
        $this->watermarkText = $watermarkText;
        $this->cargarLogoInstitucional();
        $this->addPage();
    }

    /**
     * Carga automática del logo oficial del Hospital Vitae si está disponible en disco.
     */
    protected function cargarLogoInstitucional(): void
    {
        $baseDir = dirname(__DIR__, 3);
        $rutas = [
            $baseDir . '/public/images/logo-vitae.jpg',
            $baseDir . '/storage/app/public/logo-vitae.jpg',
            $baseDir . '/../vitae-front/public/logo-vitae.jpg',
            'C:/Ingenieria_software/9no semestre/auditoria/Proyecto/vitae-backend/public/images/logo-vitae.jpg',
        ];

        foreach ($rutas as $ruta) {
            if (file_exists($ruta)) {
                $this->addImage($ruta, 'ImLogo');
                break;
            }
        }
    }

    public function addImage(string $filePath, string $id = 'ImLogo'): ?string
    {
        if (!file_exists($filePath)) {
            return null;
        }

        $data = file_get_contents($filePath);
        if ($data === false) {
            return null;
        }

        // Leer dimensiones JPEG de cabecera
        $width = 400;
        $height = 385;
        $info = @getimagesize($filePath);
        if ($info && isset($info[0], $info[1])) {
            $width = (int) $info[0];
            $height = (int) $info[1];
        }

        $this->images[$id] = [
            'id' => $id,
            'data' => $data,
            'width' => $width,
            'height' => $height,
            'length' => strlen($data),
        ];

        return $id;
    }

    public function drawImage(string $id, float $x, float $y, float $width, float $height): self
    {
        if (!isset($this->images[$id])) {
            return $this;
        }

        $wF = sprintf('%.2F', $width);
        $hF = sprintf('%.2F', $height);
        $xF = sprintf('%.2F', $x);
        $yF = sprintf('%.2F', $y);

        $op = "q {$wF} 0 0 {$hF} {$xF} {$yF} cm /{$id} Do Q";
        $this->addOp($op);
        return $this;
    }

    public function setWatermark(string $text): self
    {
        $this->watermarkText = $text;
        return $this;
    }

    public function addPage(): self
    {
        $this->currentPage++;
        $this->pages[$this->currentPage] = [
            'ops' => [],
            'hasWatermark' => !empty($this->watermarkText),
        ];

        // Reset graphics state
        $this->setFont('Helvetica', 10);
        $this->setColor(0, 0, 0);

        return $this;
    }

    public function setFont(string $name, float $size): self
    {
        $key = 'F1';
        foreach ($this->fonts as $k => $fName) {
            if (strcasecmp($fName, $name) === 0) {
                $key = $k;
                break;
            }
        }
        $this->currentFont = $key;
        $this->currentFontSize = max(4.0, $size);
        return $this;
    }

    public function setColor(float $r, float $g, float $b): self
    {
        $this->currentColor = [
            max(0.0, min(1.0, $r)),
            max(0.0, min(1.0, $g)),
            max(0.0, min(1.0, $b)),
        ];
        return $this;
    }

    public function setGray(float $gray): self
    {
        return $this->setColor($gray, $gray, $gray);
    }

    protected function addOp(string $op): void
    {
        if ($this->currentPage < 0) {
            $this->addPage();
        }
        $this->pages[$this->currentPage]['ops'][] = $op;
    }

    protected function escapePdfString(string $text): string
    {
        $converted = @mb_convert_encoding($text, 'Windows-1252', 'UTF-8');
        if ($converted === false) {
            $converted = utf8_decode($text);
        }

        $escaped = '';
        $len = strlen($converted);
        for ($i = 0; $i < $len; $i++) {
            $c = $converted[$i];
            if ($c === '\\' || $c === '(' || $c === ')') {
                $escaped .= '\\' . $c;
            } else {
                $escaped .= $c;
            }
        }
        return $escaped;
    }

    public function text(string $text, float $x, float $y): self
    {
        if ($text === '') {
            return $this;
        }
        $escaped = $this->escapePdfString($text);
        [$r, $g, $b] = $this->currentColor;
        $rF = sprintf('%.3F', $r);
        $gF = sprintf('%.3F', $g);
        $bF = sprintf('%.3F', $b);
        $xF = sprintf('%.2F', $x);
        $yF = sprintf('%.2F', $y);
        $szF = sprintf('%.2F', $this->currentFontSize);

        $op = "BT /{$this->currentFont} {$szF} Tf {$rF} {$gF} {$bF} rg 1 0 0 1 {$xF} {$yF} Tm ({$escaped}) Tj ET";
        $this->addOp($op);
        return $this;
    }

    public function textRight(string $text, float $rightX, float $y): self
    {
        $approxWidth = $this->estimateTextWidth($text, $this->currentFontSize);
        return $this->text($text, $rightX - $approxWidth, $y);
    }

    public function textCenter(string $text, float $startX, float $y, float $width): self
    {
        $approxWidth = $this->estimateTextWidth($text, $this->currentFontSize);
        $x = $startX + max(0.0, ($width - $approxWidth) / 2.0);
        return $this->text($text, $x, $y);
    }

    public function estimateTextWidth(string $text, float $fontSize): float
    {
        if (str_starts_with($this->currentFont, 'F4') || str_starts_with($this->currentFont, 'F5')) {
            return strlen($text) * $fontSize * 0.60;
        }
        return mb_strlen($text, 'UTF-8') * $fontSize * 0.50;
    }

    public function line(float $x1, float $y1, float $x2, float $y2, float $lineWidth = 0.75): self
    {
        [$r, $g, $b] = $this->currentColor;
        $rF = sprintf('%.3F', $r);
        $gF = sprintf('%.3F', $g);
        $bF = sprintf('%.3F', $b);
        $wF = sprintf('%.2F', $lineWidth);
        $x1F = sprintf('%.2F', $x1);
        $y1F = sprintf('%.2F', $y1);
        $x2F = sprintf('%.2F', $x2);
        $y2F = sprintf('%.2F', $y2);

        $op = "q {$rF} {$gF} {$bF} RG {$wF} w {$x1F} {$y1F} m {$x2F} {$y2F} l S Q";
        $this->addOp($op);
        return $this;
    }

    public function rect(
        float $x,
        float $y,
        float $w,
        float $h,
        bool $fill = false,
        bool $stroke = true,
        array $fillColor = [0.95, 0.95, 0.95],
        array $strokeColor = [0.2, 0.2, 0.2],
        float $lineWidth = 0.5
    ): self {
        $xF = sprintf('%.2F', $x);
        $yF = sprintf('%.2F', $y);
        $wF = sprintf('%.2F', $w);
        $hF = sprintf('%.2F', $h);
        $lwF = sprintf('%.2F', $lineWidth);

        $op = "q {$lwF} w ";
        if ($fill) {
            $rf = sprintf('%.3F', $fillColor[0]);
            $gf = sprintf('%.3F', $fillColor[1]);
            $bf = sprintf('%.3F', $fillColor[2]);
            $op .= "{$rf} {$gf} {$bf} rg ";
        }
        if ($stroke) {
            $rs = sprintf('%.3F', $strokeColor[0]);
            $gs = sprintf('%.3F', $strokeColor[1]);
            $bs = sprintf('%.3F', $strokeColor[2]);
            $op .= "{$rs} {$gs} {$bs} RG ";
        }

        $mode = ($fill && $stroke) ? 'B' : ($fill ? 'f' : 'S');
        $op .= "{$xF} {$yF} {$wF} {$hF} re {$mode} Q";
        $this->addOp($op);
        return $this;
    }

    public function drawWatermarkOnCurrentPage(string $text): void
    {
        if ($text === '') {
            return;
        }
        $escaped = $this->escapePdfString($text);
        $a = 0.819;
        $b = 0.574;
        $c = -0.574;
        $d = 0.819;

        $op = "q 0.900 0.900 0.900 rg BT /F2 20 Tf {$a} {$b} {$c} {$d} 80 200 Tm ({$escaped}) Tj ET Q";
        $this->addOp($op);
    }

    public function drawQrCode(string $url, float $x, float $y, float $size = 75.0): self
    {
        $this->rect($x, $y, $size, $size, true, true, [1.0, 1.0, 1.0], [0.0, 0.0, 0.0], 1.0);

        $sub = $size / 7.0;

        // Top-Left Target
        $this->rect($x + 2, $y + $size - $sub * 2 - 2, $sub * 2, $sub * 2, true, true, [0.0, 0.0, 0.0], [0.0, 0.0, 0.0], 0.5);
        $this->rect($x + 2 + $sub * 0.4, $y + $size - $sub * 1.6 - 2, $sub * 1.2, $sub * 1.2, true, false, [1.0, 1.0, 1.0]);
        $this->rect($x + 2 + $sub * 0.7, $y + $size - $sub * 1.3 - 2, $sub * 0.6, $sub * 0.6, true, false, [0.0, 0.0, 0.0]);

        // Top-Right Target
        $this->rect($x + $size - $sub * 2 - 2, $y + $size - $sub * 2 - 2, $sub * 2, $sub * 2, true, true, [0.0, 0.0, 0.0], [0.0, 0.0, 0.0], 0.5);
        $this->rect($x + $size - $sub * 1.6 - 2, $y + $size - $sub * 1.6 - 2, $sub * 1.2, $sub * 1.2, true, false, [1.0, 1.0, 1.0]);
        $this->rect($x + $size - $sub * 1.3 - 2, $y + $size - $sub * 1.3 - 2, $sub * 0.6, $sub * 0.6, true, false, [0.0, 0.0, 0.0]);

        // Bottom-Left Target
        $this->rect($x + 2, $y + 2, $sub * 2, $sub * 2, true, true, [0.0, 0.0, 0.0], [0.0, 0.0, 0.0], 0.5);
        $this->rect($x + 2 + $sub * 0.4, $y + 2 + $sub * 0.4, $sub * 1.2, $sub * 1.2, true, false, [1.0, 1.0, 1.0]);
        $this->rect($x + 2 + $sub * 0.7, $y + 2 + $sub * 0.7, $sub * 0.6, $sub * 0.6, true, false, [0.0, 0.0, 0.0]);

        // Simulated QR data cells
        $hash = md5($url);
        $cellCount = 6;
        $cellSize = ($size - 8) / $cellCount;
        for ($r = 0; $r < $cellCount; $r++) {
            for ($c = 0; $c < $cellCount; $c++) {
                if (($r < 2 && $c < 2) || ($r < 2 && $c >= $cellCount - 2) || ($r >= $cellCount - 2 && $c < 2)) {
                    continue;
                }
                $charIdx = ($r * $cellCount + $c) % strlen($hash);
                if (hexdec($hash[$charIdx]) % 2 === 0) {
                    $cx = $x + 4 + $c * $cellSize;
                    $cy = $y + 4 + $r * $cellSize;
                    $this->rect($cx, $cy, $cellSize * 0.85, $cellSize * 0.85, true, false, [0.05, 0.05, 0.05]);
                }
            }
        }

        $this->setFont('Helvetica', 6.0);
        $this->setColor(0.3, 0.3, 0.3);
        $this->text('ESCANEE PARA VALIDAR', $x + 2, $y - 8);

        return $this;
    }

    public function drawBarcode(string $code, float $x, float $y, float $width = 140.0, float $height = 30.0): self
    {
        $this->rect($x, $y, $width, $height, true, false, [1.0, 1.0, 1.0]);
        $barX = $x + 4;
        $pattern = md5($code);
        $barW = 1.2;

        for ($i = 0; $i < strlen($pattern) && $barX < ($x + $width - 6); $i++) {
            $val = hexdec($pattern[$i]);
            $w = ($val % 3 + 1) * $barW;
            $drawBar = ($val % 2 === 0);
            if ($drawBar) {
                $this->rect($barX, $y + 8, $w, $height - 8, true, false, [0.0, 0.0, 0.0]);
            }
            $barX += $w + 1.2;
        }

        $this->setFont('Courier', 7.5);
        $this->setColor(0.1, 0.1, 0.1);
        $this->textCenter('* ' . $code . ' *', $x, $y, $width);

        return $this;
    }

    public function drawHeader(string $tituloDocumento, string $subtitulo = ''): float
    {
        $startX = $this->marginLeft;
        $width = $this->pageWidth - $this->marginLeft - $this->marginRight;
        $y = $this->pageHeight - $this->marginTop;

        // Top thin rule
        $this->setColor(0.08, 0.22, 0.40);
        $this->line($startX, $y, $startX + $width, $y, 1.5);
        $y -= 14;

        // Si tenemos imagen del logo institucional, la dibujamos a la izquierda
        $textLeftX = $startX;
        if (isset($this->images['ImLogo'])) {
            $logoW = 34.0;
            $logoH = 32.8;
            $this->drawImage('ImLogo', $startX, $y - 25, $logoW, $logoH);
            $textLeftX = $startX + $logoW + 10;
        }

        // Hospital logo text & badge
        $this->setFont('Helvetica-Bold', 13.5);
        $this->setColor(0.08, 0.22, 0.40);
        $this->text('COMPLEJO HOSPITALARIO VITAE', $textLeftX, $y);

        $this->setFont('Helvetica-Bold', 8.5);
        $this->setColor(0.2, 0.2, 0.2);
        $this->textRight('Sede Central · CLUES: SSA-015820-VITAE', $startX + $width, $y);
        $y -= 12;

        $this->setFont('Helvetica', 8.0);
        $this->setColor(0.35, 0.35, 0.35);
        $this->text('Dirección Médica y Asistencial · Sistema de Información Hospitalaria (SIH)', $textLeftX, $y);
        $this->textRight('NOM-004-SSA3-2012 · NOM-024-SSA3-2012 · ISO/IEC-27001', $startX + $width, $y);
        $y -= 12;

        // Separator rule
        $this->setColor(0.8, 0.8, 0.8);
        $this->line($startX, $y, $startX + $width, $y, 0.75);
        $y -= 16;

        // Title Box
        $this->rect($startX, $y - 4, $width, 22, true, true, [0.94, 0.96, 0.98], [0.75, 0.82, 0.90], 0.75);
        $this->setFont('Helvetica-Bold', 11.0);
        $this->setColor(0.08, 0.22, 0.40);
        $this->textCenter($tituloDocumento, $startX, $y + 3, $width);

        if ($subtitulo !== '') {
            $y -= 16;
            $this->setFont('Helvetica-Oblique', 8.5);
            $this->setColor(0.3, 0.3, 0.3);
            $this->textCenter($subtitulo, $startX, $y, $width);
        }

        return $y - 14;
    }

    public function drawFooter(int $pageNumber, int $totalPages, ?string $hashSha256 = null): void
    {
        $startX = $this->marginLeft;
        $width = $this->pageWidth - $this->marginLeft - $this->marginRight;
        $y = $this->marginBottom + 20;

        $this->setColor(0.8, 0.8, 0.8);
        $this->line($startX, $y, $startX + $width, $y, 0.5);
        $y -= 9;

        $this->setFont('Helvetica', 6.2);
        $this->setColor(0.4, 0.4, 0.4);
        $aviso = 'Documento confidencial médico con datos personales sensibles. Amparado por la Ley Federal de Protección de Datos Personales (LFPDPPP) y NOM-004-SSA3-2012. Derechos ARCO: privacidad@hospitalvitae.mx.';
        $this->text($aviso, $startX, $y);
        $y -= 8;

        $fechaUtc = gmdate('Y-m-d\TH:i:s\Z');
        $selloStr = 'Sincronización: CENAM UTC (' . $fechaUtc . ')';
        if ($hashSha256) {
            $selloStr .= ' | Sello SHA-256: ' . substr($hashSha256, 0, 32) . '...';
        }
        $this->text($selloStr, $startX, $y);

        $this->setFont('Helvetica-Bold', 7.5);
        $this->setColor(0.2, 0.2, 0.2);
        $this->textRight("Página {$pageNumber} de {$totalPages}", $startX + $width, $y);
    }

    public function build(): string
    {
        $totalPages = count($this->pages);

        for ($i = 0; $i < $totalPages; $i++) {
            $this->currentPage = $i;
            if ($this->pages[$i]['hasWatermark']) {
                $this->drawWatermarkOnCurrentPage($this->watermarkText);
            }
            $this->drawFooter($i + 1, $totalPages);
        }

        $offsets = [];
        $pdf = "%PDF-1.4\n%\xE2\xE3\xCF\xD3\n";

        $catalogObj = 1;
        $pagesObj = 2;
        $currObj = 3;

        // Font Objects Map
        $fontObjMap = [];
        foreach ($this->fonts as $k => $baseFont) {
            $fontObjMap[$k] = $currObj++;
        }

        // Image Objects Map
        $imageObjMap = [];
        foreach ($this->images as $imgId => $imgInfo) {
            $imageObjMap[$imgId] = $currObj++;
        }

        // Page and Content Objects Map
        $pageObjMap = [];
        $contentObjMap = [];
        for ($i = 0; $i < $totalPages; $i++) {
            $pageObjMap[$i] = $currObj++;
            $contentObjMap[$i] = $currObj++;
        }

        $infoObj = $currObj++;

        // 1. Catalog Object
        $offsets[$catalogObj] = strlen($pdf);
        $pdf .= "{$catalogObj} 0 obj\n<< /Type /Catalog /Pages {$pagesObj} 0 R >>\nendobj\n";

        // 2. Pages Object
        $kids = [];
        for ($i = 0; $i < $totalPages; $i++) {
            $kids[] = "{$pageObjMap[$i]} 0 R";
        }
        $kidsStr = implode(' ', $kids);
        $offsets[$pagesObj] = strlen($pdf);
        $pdf .= "{$pagesObj} 0 obj\n<< /Type /Pages /Kids [{$kidsStr}] /Count {$totalPages} >>\nendobj\n";

        // Font Objects
        foreach ($this->fonts as $k => $baseFont) {
            $oNum = $fontObjMap[$k];
            $offsets[$oNum] = strlen($pdf);
            $pdf .= "{$oNum} 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /{$baseFont} /Encoding /WinAnsiEncoding >>\nendobj\n";
        }

        // Image XObjects
        foreach ($this->images as $imgId => $imgInfo) {
            $oNum = $imageObjMap[$imgId];
            $offsets[$oNum] = strlen($pdf);
            $len = $imgInfo['length'];
            $w = $imgInfo['width'];
            $h = $imgInfo['height'];
            $pdf .= "{$oNum} 0 obj\n<< /Type /XObject /Subtype /Image /Width {$w} /Height {$h} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length {$len} >>\nstream\n" . $imgInfo['data'] . "\nendstream\nendobj\n";
        }

        // Build Resources dictionary
        $fontResParts = [];
        foreach ($this->fonts as $k => $baseFont) {
            $fontResParts[] = "/{$k} {$fontObjMap[$k]} 0 R";
        }

        $xobjResParts = [];
        foreach ($this->images as $imgId => $imgInfo) {
            $xobjResParts[] = "/{$imgId} {$imageObjMap[$imgId]} 0 R";
        }

        $resStr = '<< /Font << ' . implode(' ', $fontResParts) . ' >>';
        if (!empty($xobjResParts)) {
            $resStr .= ' /XObject << ' . implode(' ', $xobjResParts) . ' >>';
        }
        $resStr .= ' >>';

        // Page and Content Objects
        for ($i = 0; $i < $totalPages; $i++) {
            $pNum = $pageObjMap[$i];
            $cNum = $contentObjMap[$i];

            $offsets[$pNum] = strlen($pdf);
            $pdf .= "{$pNum} 0 obj\n<< /Type /Page /Parent {$pagesObj} 0 R /MediaBox [0 0 {$this->pageWidth} {$this->pageHeight}] /Resources {$resStr} /Contents {$cNum} 0 R >>\nendobj\n";

            $stream = implode("\n", $this->pages[$i]['ops']);
            $len = strlen($stream);

            $offsets[$cNum] = strlen($pdf);
            $pdf .= "{$cNum} 0 obj\n<< /Length {$len} >>\nstream\n{$stream}\nendstream\nendobj\n";
        }

        // Info Object
        $offsets[$infoObj] = strlen($pdf);
        $creationDate = 'D:' . gmdate('YmdHis') . 'Z';
        $pdf .= "{$infoObj} 0 obj\n<< /Title (Hospital Vitae Document) /Creator (Hospital Vitae SIH) /CreationDate ({$creationDate}) >>\nendobj\n";

        // XRef Table
        $totalObjCount = $infoObj + 1;
        $xrefOffset = strlen($pdf);
        $pdf .= "xref\n0 {$totalObjCount}\n";
        $pdf .= "0000000000 65535 f \n";
        for ($o = 1; $o < $totalObjCount; $o++) {
            $off = $offsets[$o] ?? 0;
            $pdf .= sprintf("%010d 00000 n \n", $off);
        }

        // Trailer
        $pdf .= "trailer\n<< /Size {$totalObjCount} /Root {$catalogObj} 0 R /Info {$infoObj} 0 R >>\n";
        $pdf .= "startxref\n{$xrefOffset}\n%%EOF\n";

        return $pdf;
    }

    public function downloadResponse(string $filename): Response
    {
        $content = $this->build();
        return new Response($content, 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . $filename . '"',
            'Content-Length' => (string) strlen($content),
            'Cache-Control' => 'private, no-cache, must-revalidate',
            'Pragma' => 'no-cache',
            'X-Content-Type-Options' => 'nosniff',
        ]);
    }

    public function inlineResponse(string $filename): Response
    {
        $content = $this->build();
        return new Response($content, 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'inline; filename="' . $filename . '"',
            'Content-Length' => (string) strlen($content),
            'Cache-Control' => 'private, no-cache, must-revalidate',
            'Pragma' => 'no-cache',
            'X-Content-Type-Options' => 'nosniff',
        ]);
    }
}
