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
        $usuarios = Usuario::select([
            'id_usuario',
            'nombre_completo',
            'rol',
            'cedula',
            'estado_cuenta',
            'intentos_fallidos',
            'ultimo_acceso_en',
            'created_at',
        ])->paginate(15);

        return response()->json([
            'status' => 'success',
            'data' => $usuarios,
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
}
