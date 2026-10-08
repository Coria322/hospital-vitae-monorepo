<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Auditoria extends Model
{
    protected $fillable = [
        'titulo',
        'descripcion',
        'auditor',
        'estado',
        'fecha_inicio',
    ];
}
