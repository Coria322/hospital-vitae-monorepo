<?php

namespace Tests\Unit;

use App\Services\Pdf\PdfDocumentBuilder;
use PHPUnit\Framework\TestCase;

class PdfDocumentBuilderTest extends TestCase
{
    public function test_genera_estructura_pdf_valida(): void
    {
        $builder = new PdfDocumentBuilder('MARCA DE AGUA AUDITORIA');
        $y = $builder->drawHeader('RECETA MÉDICA INSTITUCIONAL', 'Subtítulo de prueba');
        $builder->text('Paciente: Juan Pérez', 36, $y);
        $builder->drawQrCode('https://vitae.hospital/verificar-receta?folio=REC-1234', 480, $y - 70, 60);

        $pdf = $builder->build();

        $this->assertStringStartsWith('%PDF-1.4', $pdf);
        $this->assertStringEndsWith("%%EOF\n", $pdf);
        $this->assertStringContainsString('/Type /Catalog', $pdf);
        $this->assertStringContainsString('/Type /Pages', $pdf);
        $this->assertStringContainsString('/Type /Page', $pdf);
        $this->assertStringContainsString('COMPLEJO HOSPITALARIO VITAE', $pdf);
        $this->assertGreaterThan(1000, strlen($pdf));
    }
}
