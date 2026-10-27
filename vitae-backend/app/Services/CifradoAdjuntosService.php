<?php

namespace App\Services;

use Exception;
use Illuminate\Support\Facades\Storage;
use RuntimeException;

class CifradoAdjuntosService
{
    /**
     * Algoritmo de cifrado estándar de grado militar (RNF-003 / ISO/IEC-27001).
     */
    protected const CIPHER_ALGO = 'aes-256-gcm';

    /**
     * Longitud del Vector de Inicialización (IV) recomendado por NIST para GCM (96 bits = 12 bytes).
     */
    protected const IV_LENGTH = 12;

    /**
     * Longitud del Tag de Autenticación de integridad (128 bits = 16 bytes).
     */
    protected const TAG_LENGTH = 16;

    /**
     * Magic header identificador para archivos cifrados de Hospital Vitae ("VENC\x01").
     */
    protected const MAGIC_HEADER = "VENC\x01";

    /**
     * Obtiene la clave binaria de 256 bits (32 bytes) derivada de la clave de aplicación.
     */
    protected function obtenerLlaveCifrado(?string $llavePersonalizada = null): string
    {
        if ($llavePersonalizada !== null) {
            return hash('sha256', $llavePersonalizada, true);
        }

        $appKey = config('app.key');
        if (empty($appKey)) {
            throw new RuntimeException("Clave de cifrado no configurada (APP_KEY vacía).");
        }

        // Si la clave tiene prefijo 'base64:', decodificarla
        if (str_starts_with($appKey, 'base64:')) {
            $keyRaw = base64_decode(substr($appKey, 7));
            return hash('sha256', $keyRaw, true);
        }

        return hash('sha256', $appKey, true);
    }

    /**
     * Cifra un contenido binario (documento, PDF, imagen clínica) usando AES-256-GCM (TASK-W-027).
     *
     * Estructura del blob resultante:
     * [5 bytes: MAGIC (VENC\x01)] + [12 bytes: IV] + [16 bytes: TAG] + [N bytes: CIPHERTEXT]
     */
    public function cifrar(string $contenidoPlano, ?string $llavePersonalizada = null): string
    {
        $key = $this->obtenerLlaveCifrado($llavePersonalizada);
        $iv = random_bytes(self::IV_LENGTH);
        $tag = '';

        $ciphertext = openssl_encrypt(
            $contenidoPlano,
            self::CIPHER_ALGO,
            $key,
            OPENSSL_RAW_DATA,
            $iv,
            $tag,
            '',
            self::TAG_LENGTH
        );

        if ($ciphertext === false) {
            throw new RuntimeException("Fallo en la operación de cifrado AES-256-GCM.");
        }

        return self::MAGIC_HEADER . $iv . $tag . $ciphertext;
    }

    /**
     * Descifra un blob cifrado verificando previamente la integridad del Authentication Tag (GCM).
     */
    public function descifrar(string $blobCifrado, ?string $llavePersonalizada = null): string
    {
        $key = $this->obtenerLlaveCifrado($llavePersonalizada);

        $headerLen = strlen(self::MAGIC_HEADER);
        $minLen = $headerLen + self::IV_LENGTH + self::TAG_LENGTH;

        if (strlen($blobCifrado) < $minLen) {
            throw new RuntimeException("El archivo o blob proporcionado no tiene el tamaño mínimo requerido.");
        }

        $header = substr($blobCifrado, 0, $headerLen);
        if ($header !== self::MAGIC_HEADER) {
            throw new RuntimeException("Encabezado de archivo inválido. El contenido no es un adjunto cifrado de Hospital Vitae.");
        }

        $iv = substr($blobCifrado, $headerLen, self::IV_LENGTH);
        $tag = substr($blobCifrado, $headerLen + self::IV_LENGTH, self::TAG_LENGTH);
        $ciphertext = substr($blobCifrado, $headerLen + self::IV_LENGTH + self::TAG_LENGTH);

        $plaintext = openssl_decrypt(
            $ciphertext,
            self::CIPHER_ALGO,
            $key,
            OPENSSL_RAW_DATA,
            $iv,
            $tag
        );

        if ($plaintext === false) {
            throw new RuntimeException("Autenticación o descifrado fallido. El archivo fue alterado o la clave es incorrecta.");
        }

        return $plaintext;
    }

    /**
     * Almacena un documento clínico cifrado en el disco seguro especificado.
     */
    public function almacenarAdjuntoCifrado(string $rutaRelativa, string $contenidoPlano, string $disco = 'local'): bool
    {
        $blobCifrado = $this->cifrar($contenidoPlano);
        return Storage::disk($disco)->put($rutaRelativa, $blobCifrado);
    }

    /**
     * Recupera y descifra en memoria un documento clínico sin persistirlo en claro en disco.
     */
    public function obtenerAdjuntoDescifrado(string $rutaRelativa, string $disco = 'local'): string
    {
        $blobCifrado = Storage::disk($disco)->get($rutaRelativa);
        if ($blobCifrado === null) {
            throw new RuntimeException("El archivo {$rutaRelativa} no existe en el disco {$disco}.");
        }

        return $this->descifrar($blobCifrado);
    }
}
