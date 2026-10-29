package com.vyra.dental.dao.memoria;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.UsuarioDAO;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.util.Validador;

/**
 * Implementacion en memoria (ArrayList) de UsuarioDAO.
 * Para las pruebas de los servicios y los demos: los datos se pierden al cerrar la aplicacion.
 */
public class UsuarioDAOMemoria implements UsuarioDAO {

    private final List<Usuario> usuarios = new ArrayList<>();
    private int siguienteId = 1;

    @Override
    public Usuario crear(Usuario usuario) {
        Validador.validar(usuario);
        usuario.setIdUsuario(siguienteId++);
        usuarios.add(usuario);
        return usuario;
    }

    @Override
    public Optional<Usuario> buscarPorId(int idUsuario) {
        return usuarios.stream().filter(u -> u.getIdUsuario() == idUsuario).findFirst();
    }

    @Override
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        return usuarios.stream().filter(u -> u.getNombreUsuario().equals(nombreUsuario)).findFirst();
    }

    @Override
    public List<Usuario> listarTodos() {
        return new ArrayList<>(usuarios);
    }

    @Override
    public int contar() {
        return usuarios.size();
    }

    @Override
    public boolean actualizar(Usuario usuario) {
        Validador.validar(usuario);
        for (int i = 0; i < usuarios.size(); i++) {
            if (usuarios.get(i).getIdUsuario() == usuario.getIdUsuario()) {
                usuarios.set(i, usuario);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean eliminar(int idUsuario) {
        return usuarios.removeIf(u -> u.getIdUsuario() == idUsuario);
    }
}
