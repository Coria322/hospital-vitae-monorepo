<?php

namespace App\Http\Controllers;

use App\Models\Auditoria;
use Illuminate\Http\Request;

class AuditoriaController extends Controller
{
    /**
     * Display a listing of the resource.
     */
    public function index()
    {
        return response()->json([
            'status' => 'success',
            'data' => Auditoria::all()
        ], 200);
    }

    /**
     * Store a newly created resource in storage.
     */
    public function store(Request $request)
    {
        $validated = $request->validate([
            'titulo' => 'required|string|max:255',
            'descripcion' => 'nullable|string',
            'auditor' => 'required|string|max:255',
            'estado' => 'nullable|string',
            'fecha_inicio' => 'nullable|date',
        ]);

        $auditoria = Auditoria::create($validated);

        return response()->json([
            'status' => 'success',
            'message' => 'Auditoría creada correctamente en la base de datos',
            'data' => $auditoria
        ], 201);
    }

    /**
     * Display the specified resource.
     */
    public function show(Auditoria $auditoria)
    {
        return response()->json([
            'status' => 'success',
            'data' => $auditoria
        ], 200);
    }

    /**
     * Update the specified resource in storage.
     */
    public function update(Request $request, Auditoria $auditoria)
    {
        $validated = $request->validate([
            'titulo' => 'sometimes|required|string|max:255',
            'descripcion' => 'nullable|string',
            'auditor' => 'sometimes|required|string|max:255',
            'estado' => 'nullable|string',
            'fecha_inicio' => 'nullable|date',
        ]);

        $auditoria->update($validated);

        return response()->json([
            'status' => 'success',
            'message' => 'Auditoría actualizada correctamente',
            'data' => $auditoria
        ], 200);
    }

    /**
     * Remove the specified resource from storage.
     */
    public function destroy(Auditoria $auditoria)
    {
        $auditoria->delete();

        return response()->json([
            'status' => 'success',
            'message' => 'Auditoría eliminada de la base de datos'
        ], 200);
    }
}
