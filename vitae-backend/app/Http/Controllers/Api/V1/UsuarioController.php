<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Http\Requests\V1\StoreUsuarioRequest;
use App\Models\Usuario;
use App\Services\UsuarioService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class UsuarioController extends Controller
{
    public function __construct(
        protected UsuarioService $usuarioService
    ) {}

    public function index(Request $request): JsonResponse
    {
        $perPage = min(max((int) $request->input('per_page', 100), 1), 200);

        $usuarios = Usuario::select([
            'id_usuario',
            'nombre_completo',
            'rol',
            'cedula',
            'estado_cuenta',
            'intentos_fallidos',
            'ultimo_acceso_en',
            'created_at',
        ])->paginate($perPage);

        return response()->json([
            'status' => 'success',
            'data' => $usuarios,
        ], 200);
    }

    public function proximoId(Request $request): JsonResponse
    {
        $rol = $request->query('rol', 'MEDICO');
        $siguienteId = $this->usuarioService->generarSiguienteIdUsuario($rol);

        return response()->json([
            'status' => 'success',
            'data' => [
                'rol' => $rol,
                'proximo_id' => $siguienteId,
            ],
        ], 200);
    }

    public function store(StoreUsuarioRequest $request): JsonResponse
    {
        $usuario = $this->usuarioService->crearUsuario($request->validated());

        return response()->json([
            'status' => 'success',
            'message' => 'Usuario registrado exitosamente',
            'data' => [
                'id_usuario' => $usuario->id_usuario,
                'nombre_completo' => $usuario->nombre_completo,
                'rol' => $usuario->rol,
                'cedula' => $usuario->cedula,
                'estado_cuenta' => $usuario->estado_cuenta,
            ],
        ], 201);
    }

    public function show(string $id): JsonResponse
    {
        $usuario = Usuario::where('id_usuario', $id)->first();

        if (!$usuario) {
            return response()->json([
                'status' => 'error',
                'message' => 'Usuario no encontrado',
            ], 404);
        }

        return response()->json([
            'status' => 'success',
            'data' => [
                'id_usuario' => $usuario->id_usuario,
                'nombre_completo' => $usuario->nombre_completo,
                'rol' => $usuario->rol,
                'cedula' => $usuario->cedula,
                'estado_cuenta' => $usuario->estado_cuenta,
                'ultimo_acceso_en' => $usuario->ultimo_acceso_en,
            ],
        ], 200);
    }

    public function baja(string $id): JsonResponse
    {
        $usuario = Usuario::where('id_usuario', $id)->first();

        if (!$usuario) {
            return response()->json([
                'status' => 'error',
                'message' => 'Usuario no encontrado',
            ], 404);
        }

        $this->usuarioService->darDeBaja($usuario);

        return response()->json([
            'status' => 'success',
            'message' => 'Usuario dado de baja exitosamente. Sesiones revocadas.',
        ], 200);
    }

    public function reactivar(string $id): JsonResponse
    {
        $usuario = Usuario::where('id_usuario', $id)->first();

        if (!$usuario) {
            return response()->json([
                'status' => 'error',
                'message' => 'Usuario no encontrado',
            ], 404);
        }

        $this->usuarioService->reactivar($usuario);

        return response()->json([
            'status' => 'success',
            'message' => 'Cuenta de usuario reactivada correctamente. Se requerirá cambio de contraseña.',
        ], 200);
    }

    public function bloquear(string $id): JsonResponse
    {
        $usuario = Usuario::where('id_usuario', $id)->first();

        if (!$usuario) {
            return response()->json([
                'status' => 'error',
                'message' => 'Usuario no encontrado',
            ], 404);
        }

        $this->usuarioService->bloquear($usuario);

        return response()->json([
            'status' => 'success',
            'message' => 'Cuenta de usuario bloqueada preventivamente.',
        ], 200);
    }

    public function update(Request $request, string $id): JsonResponse
    {
        $usuario = Usuario::where('id_usuario', $id)->first();

        if (!$usuario) {
            return response()->json([
                'status' => 'error',
                'message' => 'Usuario no encontrado',
            ], 404);
        }

        $validated = $request->validate([
            'nombre_completo' => 'sometimes|string|max:100',
            'rol' => 'sometimes|string|in:ADMINISTRADOR,MEDICO,ENFERMERIA,RECEPCION,LABORATORISTA,LABORATORIO,AUDITOR',
            'cedula' => 'nullable|string|size:10',
            'estado_cuenta' => 'sometimes|string|in:ACTIVA,INACTIVA,BLOQUEADA,BAJA',
            'password' => 'nullable|string|min:8',
        ]);

        $usuario = $this->usuarioService->actualizarUsuario($usuario, $validated);

        return response()->json([
            'status' => 'success',
            'message' => 'Usuario actualizado exitosamente',
            'data' => [
                'id_usuario' => $usuario->id_usuario,
                'nombre_completo' => $usuario->nombre_completo,
                'rol' => $usuario->rol,
                'cedula' => $usuario->cedula,
                'estado_cuenta' => $usuario->estado_cuenta,
            ],
        ], 200);
    }

    public function resetPassword(string $id, Request $request): JsonResponse
    {
        $usuario = Usuario::where('id_usuario', $id)->first();

        if (!$usuario) {
            return response()->json([
                'status' => 'error',
                'message' => 'Usuario no encontrado',
            ], 404);
        }

        $passwordProvisional = $request->input('password') ?: 'Vitae.' . rand(100000, 999999) . '!';
        $this->usuarioService->actualizarUsuario($usuario, [
            'password' => $passwordProvisional,
            'estado_cuenta' => 'ACTIVA',
        ]);
        $usuario->update(['intentos_fallidos' => 0]);

        \App\Services\AuditLogger::log(
            tipoMovimiento: 'RESET_PASSWORD',
            tablaAfectada: 'usuario',
            registroId: $usuario->id_usuario,
            valorAnterior: null,
            valorNuevo: ['password' => 'RESETEADA', 'intentos_fallidos' => 0],
            resultado: 'EXITOSO',
            motivo: 'Reemision de credencial provisional por administrador'
        );

        return response()->json([
            'status' => 'success',
            'message' => "Credencial provisional generada exitosamente para el usuario {$usuario->id_usuario}.",
            'data' => [
                'id_usuario' => $usuario->id_usuario,
                'password_provisional' => $passwordProvisional,
            ],
        ], 200);
    }

    /**
     * Catálogo institucional de médicos activos para agendamiento de citas y asignaciones.
     */
    public function medicos(): JsonResponse
    {
        $medicos = Usuario::where('rol', 'MEDICO')
            ->where('estado_cuenta', 'ACTIVA')
            ->select(['id_usuario', 'nombre_completo', 'rol', 'cedula'])
            ->orderBy('nombre_completo', 'asc')
            ->get();

        return response()->json([
            'status' => 'success',
            'data' => $medicos,
        ], 200);
    }
}
