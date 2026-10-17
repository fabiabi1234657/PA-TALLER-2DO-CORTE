import { useEffect, useState } from 'react'

const apiUrl = (import.meta.env.VITE_API_URL || 'http://127.0.0.1:8081').replace(/\/$/, '')
const formularioVacio = { name: '', price: '', description: '' }

async function solicitar(ruta, opciones) {
  const respuesta = await fetch(`${apiUrl}${ruta}`, opciones)
  if (!respuesta.ok) {
    const detalle = respuesta.status === 400
      ? await respuesta.text()
      : respuesta.status === 500
        ? 'Error del backend: comprueba que el motor de base de datos correspondiente esté activo.'
        : `Error HTTP ${respuesta.status}`
    throw new Error(detalle)
  }
  return respuesta.status === 204 ? null : respuesta.json()
}

function App() {
  const [motor, setMotor] = useState('sql') // 'sql' | 'nosql'
  const [productos, setProductos] = useState([])
  const [formulario, setFormulario] = useState(formularioVacio)
  const [editandoId, setEditandoId] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')

  // Estado para filtros NoSQL y visor de pipeline
  const [tipoFiltro, setTipoFiltro] = useState('todos')
  const [filtroMin, setFiltroMin] = useState('50000')
  const [filtroMax, setFiltroMax] = useState('2000000')
  const [filtroTexto, setFiltroTexto] = useState('')
  const [filtroLimite, setFiltroLimite] = useState('3')
  const [pipelineActivo, setPipelineActivo] = useState(null)

  const rutaBase = motor === 'sql' ? '/productos' : '/productos/nosql'

  async function cargarProductos() {
    setCargando(true)
    setError('')
    try {
      const datos = await solicitar(rutaBase)
      setProductos(datos)
      if (motor === 'nosql') {
        setPipelineActivo(`[\n  { "$sort": { "id": 1 } }\n]`)
      } else {
        setPipelineActivo(null)
      }
    } catch (fallo) {
      setError(fallo.message === 'Failed to fetch'
        ? `No se pudo conectar con Spring Boot en el puerto 8081 (modo ${motor.toUpperCase()}).`
        : fallo.message)
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    cancelar()
    setTipoFiltro('todos')
    cargarProductos()
  }, [motor])

  function cambiarCampo(evento) {
    setFormulario({ ...formulario, [evento.target.name]: evento.target.value })
  }

  function editar(producto) {
    setEditandoId(producto.id)
    setFormulario({
      name: producto.name,
      price: String(producto.price),
      description: producto.description
    })
    setMensaje('')
    setError('')
  }

  function cancelar() {
    setEditandoId(null)
    setFormulario(formularioVacio)
  }

  async function guardar(evento) {
    evento.preventDefault()
    setGuardando(true)
    setError('')
    setMensaje('')
    try {
      const producto = {
        name: formulario.name.trim(),
        price: Number(formulario.price),
        description: formulario.description.trim()
      }
      const ruta = editandoId === null ? rutaBase : `${rutaBase}/${editandoId}`
      await solicitar(ruta, {
        method: editandoId === null ? 'POST' : 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(producto)
      })
      setMensaje(editandoId === null
        ? `Producto creado en ${motor.toUpperCase()}.`
        : `Producto actualizado en ${motor.toUpperCase()}.`)
      cancelar()
      await cargarProductos()
    } catch (fallo) {
      setError(fallo.message)
    } finally {
      setGuardando(false)
    }
  }

  async function eliminar(producto) {
    if (!window.confirm(`¿Eliminar ${producto.name} de ${motor.toUpperCase()}?`)) return
    setError('')
    setMensaje('')
    try {
      await solicitar(`${rutaBase}/${producto.id}`, { method: 'DELETE' })
      if (editandoId === producto.id) cancelar()
      setMensaje(`Producto eliminado de ${motor.toUpperCase()}.`)
      await cargarProductos()
    } catch (fallo) {
      setError(fallo.message)
    }
  }

  // Ejecución de los 4 métodos de filtrado con pipeline de MongoDB
  async function aplicarFiltro(tipo = tipoFiltro) {
    setCargando(true)
    setError('')
    setMensaje('')
    try {
      if (tipo === 'todos') {
        await cargarProductos()
        return
      }

      let urlFiltro = ''
      if (tipo === 'rango') {
        urlFiltro = `/productos/nosql/filtros/rango?min=${filtroMin || 0}&max=${filtroMax || 10000000}`
      } else if (tipo === 'palabra') {
        urlFiltro = `/productos/nosql/filtros/palabra?texto=${encodeURIComponent(filtroTexto)}`
      } else if (tipo === 'top') {
        urlFiltro = `/productos/nosql/filtros/top?limite=${filtroLimite || 3}`
      } else if (tipo === 'promedio') {
        urlFiltro = `/productos/nosql/filtros/promedio`
      }

      const respuesta = await solicitar(urlFiltro)
      setProductos(respuesta.productos || [])
      setPipelineActivo(respuesta.pipeline)
      setMensaje(`Filtro "${respuesta.filtro}" aplicado exitosamente.`)
    } catch (fallo) {
      setError(fallo.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <main className="contenedor">
      <header>
        <p className="etiqueta">Spring Boot + React + Arquitectura Hexagonal</p>
        <h1>CRUD Multi-Persistencia</h1>
        <p>Gestión desacoplada de productos con persistencia intercambiable en SQL (MySQL) y NoSQL (MongoDB).</p>
      </header>

      {/* Menú de selección de persistencia */}
      <nav className="menu-bd" aria-label="Selector de persistencia">
        <button
          type="button"
          className={`btn-tab ${motor === 'sql' ? 'activo' : ''}`}
          onClick={() => setMotor('sql')}
        >
          🐬 Relacional (MySQL / SQL)
          {motor === 'sql' && <span className="badge badge-sql">Activo</span>}
        </button>
        <button
          type="button"
          className={`btn-tab ${motor === 'nosql' ? 'activo' : ''}`}
          onClick={() => setMotor('nosql')}
        >
          🍃 NoSQL (MongoDB / Aggregations)
          {motor === 'nosql' && <span className="badge badge-nosql">Activo</span>}
        </button>
      </nav>

      {/* Sección NoSQL: Filtros con Pipelines de Agregación */}
      {motor === 'nosql' && (
        <section className="tarjeta panel-filtros" aria-labelledby="titulo-filtros">
          <h2 id="titulo-filtros">Consultas Avanzadas con Pipelines de MongoDB (Aggregate)</h2>
          <p style={{ color: '#53647d', marginTop: '-10px', marginBottom: '16px' }}>
            Selecciona uno de los 4 filtros basados en pipelines de agregación nativos de MongoDB:
          </p>

          <div className="filtros-opciones">
            <button
              type="button"
              className={tipoFiltro === 'rango' ? '' : 'secundario'}
              onClick={() => { setTipoFiltro('rango'); aplicarFiltro('rango') }}
            >
              💰 1. Rango de Precios
            </button>
            <button
              type="button"
              className={tipoFiltro === 'palabra' ? '' : 'secundario'}
              onClick={() => { setTipoFiltro('palabra'); aplicarFiltro('palabra') }}
            >
              🔍 2. Por Palabra Clave
            </button>
            <button
              type="button"
              className={tipoFiltro === 'top' ? '' : 'secundario'}
              onClick={() => { setTipoFiltro('top'); aplicarFiltro('top') }}
            >
              🏆 3. Top N Más Caros
            </button>
            <button
              type="button"
              className={tipoFiltro === 'promedio' ? '' : 'secundario'}
              onClick={() => { setTipoFiltro('promedio'); aplicarFiltro('promedio') }}
            >
              📊 4. Superiores al Promedio
            </button>
            <button
              type="button"
              className={tipoFiltro === 'todos' ? '' : 'secundario'}
              onClick={() => { setTipoFiltro('todos'); aplicarFiltro('todos') }}
            >
              🔄 Listar Todos
            </button>
          </div>

          {/* Parámetros dinámicos según el filtro seleccionado */}
          {tipoFiltro === 'rango' && (
            <div className="filtro-campos">
              <label>Precio mínimo:
                <input type="number" value={filtroMin} onChange={e => setFiltroMin(e.target.value)} />
              </label>
              <label>Precio máximo:
                <input type="number" value={filtroMax} onChange={e => setFiltroMax(e.target.value)} />
              </label>
              <button type="button" onClick={() => aplicarFiltro('rango')}>Ejecutar Pipeline</button>
            </div>
          )}

          {tipoFiltro === 'palabra' && (
            <div className="filtro-campos">
              <label>Palabra o término:
                <input type="text" placeholder="Ej: Portátil, USB..." value={filtroTexto} onChange={e => setFiltroTexto(e.target.value)} />
              </label>
              <button type="button" onClick={() => aplicarFiltro('palabra')}>Ejecutar Pipeline</button>
            </div>
          )}

          {tipoFiltro === 'top' && (
            <div className="filtro-campos">
              <label>Límite de productos:
                <input type="number" min="1" max="50" value={filtroLimite} onChange={e => setFiltroLimite(e.target.value)} />
              </label>
              <button type="button" onClick={() => aplicarFiltro('top')}>Ejecutar Pipeline</button>
            </div>
          )}

          {/* Visor del código del pipeline de agregación ejecutado */}
          {pipelineActivo && (
            <div className="pipeline-visor">
              <h4>Código del Pipeline de Agregación MongoDB Empleado:</h4>
              <pre className="pipeline-codigo">{pipelineActivo}</pre>
            </div>
          )}
        </section>
      )}

      {/* Formulario CRUD */}
      <section className="tarjeta" aria-labelledby="titulo-formulario">
        <h2 id="titulo-formulario">
          {editandoId === null ? `Nuevo producto en ${motor.toUpperCase()}` : `Editar producto #${editandoId} en ${motor.toUpperCase()}`}
        </h2>
        <form onSubmit={guardar}>
          <label>Nombre
            <input name="name" value={formulario.name} onChange={cambiarCampo} required maxLength="100" placeholder="Nombre del producto" />
          </label>
          <label>Precio
            <input name="price" type="number" value={formulario.price} onChange={cambiarCampo} required min="0" step="0.01" placeholder="Ej: 85000" />
          </label>
          <label className="ancho">Descripción
            <input name="description" value={formulario.description} onChange={cambiarCampo} maxLength="255" placeholder="Descripción detallada" />
          </label>
          <div className="acciones ancho">
            <button type="submit" disabled={guardando}>
              {guardando ? 'Guardando...' : editandoId === null ? `Crear en ${motor.toUpperCase()}` : 'Guardar cambios'}
            </button>
            {editandoId !== null && <button type="button" className="secundario" onClick={cancelar}>Cancelar</button>}
          </div>
        </form>
      </section>

      {error && <p role="alert" className="aviso error">{error}</p>}
      {mensaje && <p role="status" className="aviso exito">{mensaje}</p>}

      {/* Tabla de Productos */}
      <section className="tarjeta" aria-labelledby="titulo-lista">
        <div className="cabecera-lista">
          <h2 id="titulo-lista">
            Productos en {motor.toUpperCase()} ({productos.length})
          </h2>
          <button type="button" className="secundario" onClick={() => aplicarFiltro('todos')}>
            Actualizar lista
          </button>
        </div>
        {cargando ? <p>Cargando productos de {motor.toUpperCase()}...</p> : (
          <div className="tabla-contenedor">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Nombre</th>
                  <th>Precio</th>
                  <th>Descripción</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {productos.map((producto) => (
                  <tr key={producto.id}>
                    <td><strong>#{producto.id}</strong></td>
                    <td>{producto.name}</td>
                    <td>${Number(producto.price).toLocaleString()}</td>
                    <td>{producto.description}</td>
                    <td className="acciones-fila">
                      <button type="button" className="secundario" onClick={() => editar(producto)}>Editar</button>
                      <button type="button" className="peligro" onClick={() => eliminar(producto)}>Eliminar</button>
                    </td>
                  </tr>
                ))}
                {productos.length === 0 && (
                  <tr>
                    <td colSpan="5">No se encontraron productos en {motor.toUpperCase()}.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  )
}

export default App
