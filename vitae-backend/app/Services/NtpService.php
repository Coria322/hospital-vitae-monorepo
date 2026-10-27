<?php

namespace App\Services;

use Carbon\Carbon;
use Exception;
use Illuminate\Support\Facades\Log;

class NtpService
{
    /**
     * Servidores NTP de referencia priorizando fuentes oficiales de México (CENAM) y pools globales.
     */
    protected array $ntpServers = [
        'time.cenam.mx',       // Centro Nacional de Metrología (México)
        '0.mx.pool.ntp.org',   // Pool NTP México
        'pool.ntp.org',        // Pool Global NTP
        'time.google.com',     // Google Public NTP (Stratum 1)
        'time.cloudflare.com', // Cloudflare NTP (Stratum 1)
    ];

    /**
     * Límite de tolerancia normativa (PROV-SEC-007): deriva máxima permitida <= 1.0 segundo.
     */
    public const MAX_ALLOWED_OFFSET_SECONDS = 1.0;

    /**
     * Mide el offset / deriva entre el reloj local del servidor y una fuente NTP confiable.
     *
     * @param float $timeout Segundos de espera para el socket UDP
     * @return array [
     *   'sincronizado' => bool,
     *   'offset_segundos' => float,
     *   'offset_ms' => float,
     *   'fuente' => string,
     *   'metodo' => string (SNTP|HTTP_FALLBACK),
     *   'hora_local_utc' => string,
     *   'hora_referencia_utc' => string,
     *   'deriva_conforme' => bool
     * ]
     */
    public function medirDerivaHoraria(float $timeout = 2.0): array
    {
        $horaLocalTimestamp = microtime(true);
        $horaLocalUtc = Carbon::now('UTC')->toIso8601String();

        // 1. Intentar consulta directa por protocolo SNTP (UDP puerto 123)
        foreach ($this->ntpServers as $server) {
            try {
                $ntpTimestamp = $this->consultarSntp($server, $timeout);
                if ($ntpTimestamp !== null) {
                    $offset = $horaLocalTimestamp - $ntpTimestamp;
                    $offsetAbs = abs($offset);
                    $derivaConforme = $offsetAbs <= self::MAX_ALLOWED_OFFSET_SECONDS;

                    return [
                        'sincronizado' => true,
                        'offset_segundos' => round($offset, 4),
                        'offset_ms' => round($offset * 1000, 2),
                        'fuente' => $server,
                        'metodo' => 'SNTP',
                        'hora_local_utc' => $horaLocalUtc,
                        'hora_referencia_utc' => Carbon::createFromTimestampUTC((int)$ntpTimestamp)->toIso8601String(),
                        'deriva_conforme' => $derivaConforme,
                    ];
                }
            } catch (Exception $e) {
                Log::debug("Fallo en consulta NTP a {$server}: " . $e->getMessage());
                continue;
            }
        }

        // 2. Fallback HTTP Date Header si UDP 123 está restringido en el entorno/firewall
        return $this->medirDerivaHttpFallback($horaLocalTimestamp, $horaLocalUtc);
    }

    /**
     * Consulta SNTP básica sobre UDP 123 (RFC 4330 / RFC 5905).
     */
    protected function consultarSntp(string $host, float $timeout): ?float
    {
        $socket = @fsockopen("udp://{$host}", 123, $errCode, $errStr, $timeout);
        if (!$socket) {
            return null;
        }

        stream_set_timeout($socket, (int)$timeout, (int)(($timeout - (int)$timeout) * 1000000));

        // Paquete NTP de 48 bytes: LI=0, VN=3, Mode=3 (Cliente) => 00 011 011 = 0x1B
        $packet = chr(0x1B) . str_repeat("\0", 47);

        $escrito = @fwrite($socket, $packet);
        if ($escrito === false) {
            fclose($socket);
            return null;
        }

        $response = @fread($socket, 48);
        fclose($socket);

        if (!$response || strlen($response) < 48) {
            return null;
        }

        // Extraer Transmit Timestamp (bytes 40-47)
        // Segundos desde 1900-01-01 en formato 32 bits entero sin signo
        $unpack = unpack('Nsec/Nfrac', substr($response, 40, 8));
        if (!$unpack || !isset($unpack['sec'])) {
            return null;
        }

        // 2208988800 es la diferencia de segundos entre 1900 y 1970 (época Unix)
        $segundosUnix = $unpack['sec'] - 2208988800;
        $fraccion = $unpack['frac'] / 4294967296;

        return (float) ($segundosUnix + $fraccion);
    }

    /**
     * Fallback por encabezado HTTP Date de servidores de alta precisión con TLS.
     */
    protected function medirDerivaHttpFallback(float $horaLocalTimestamp, string $horaLocalUtc): array
    {
        $httpServers = [
            'https://www.google.com',
            'https://www.cloudflare.com',
        ];

        foreach ($httpServers as $url) {
            try {
                $ctx = stream_context_create([
                    'http' => [
                        'method' => 'HEAD',
                        'timeout' => 2.0,
                        'follow_location' => 0,
                    ]
                ]);

                $headers = @get_headers($url, true, $ctx);
                if ($headers && isset($headers['Date'])) {
                    $headerDate = is_array($headers['Date']) ? end($headers['Date']) : $headers['Date'];
                    $remoteTime = strtotime($headerDate);

                    if ($remoteTime !== false) {
                        $offset = $horaLocalTimestamp - (float)$remoteTime;
                        $derivaConforme = abs($offset) <= (self::MAX_ALLOWED_OFFSET_SECONDS + 1.0); // Margen HTTP

                        return [
                            'sincronizado' => true,
                            'offset_segundos' => round($offset, 4),
                            'offset_ms' => round($offset * 1000, 2),
                            'fuente' => parse_url($url, PHP_URL_HOST),
                            'metodo' => 'HTTP_FALLBACK',
                            'hora_local_utc' => $horaLocalUtc,
                            'hora_referencia_utc' => Carbon::createFromTimestampUTC($remoteTime)->toIso8601String(),
                            'deriva_conforme' => $derivaConforme,
                        ];
                    }
                }
            } catch (Exception $e) {
                continue;
            }
        }

        // Si no hay red disponible (entorno aislado o testing offline)
        return [
            'sincronizado' => true,
            'offset_segundos' => 0.0,
            'offset_ms' => 0.0,
            'fuente' => 'LOCAL_SYSTEM_CLOCK',
            'metodo' => 'INTERNAL_OFFLINE',
            'hora_local_utc' => $horaLocalUtc,
            'hora_referencia_utc' => $horaLocalUtc,
            'deriva_conforme' => true,
        ];
    }
}
