package practica_busqueda;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Agent extends BaseAgent {
    //Pathfinder
    private PathFinder pf, pf_10_gemas;
    private ArrayList<Node> path  = new ArrayList<>();
    private ArrayList<Observation>[][] grid;

    private Vector2d ultimaPos;

    // Auxiliar variables
    private Vector2d fescala;
    private Boolean actualizarmapa = true;
    private int nGemas = 0;
    private int ticks_stopped = 0;
    private int ticks_sin_caminos = 0;
    private Boolean objetivo_rocas = false;

    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        super(stateObs, elapsedTimer);

        //Creamos una lista de IDs de obstaculos
        ArrayList<Integer> tiposObs = new ArrayList();
        tiposObs.add(0); //<- Muros
        tiposObs.add(7); //<- Piedras

        //Se inicializa el objeto del pathfinder con las ids de los obstaculos
        pf = new PathFinder(tiposObs);
        pf.VERBOSE = false; // <- activa o desactiva el modo la impresion del log

        //Se lanza el algoritmo de pathfinding para poder ser usado en la funcion aCT
        pf.run(stateObs);

        // inicialización del pathfinder cuando tienes 10 gemas
        ArrayList<Integer> tiposObs10gems = new ArrayList();
        tiposObs10gems.add(0); //<- Muros
        tiposObs10gems.add(7); //<- Piedras
        tiposObs10gems.add(6); //<- Gemas

        //Se inicializa el objeto del pathfinder con las ids de los obstaculos
        pf_10_gemas = new PathFinder(tiposObs10gems);
        pf_10_gemas.VERBOSE = false;

        //Se lanza el algoritmo de pathfinding para poder ser usado en la funcion aCT
        pf_10_gemas.run(stateObs);

        //Calculamos el factor de escala entre mundos
        fescala = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length , stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

        //Ultima posicion del avatar
        ultimaPos = new Vector2d(stateObs.getAvatarPosition().x / fescala.x, stateObs.getAvatarPosition().y / fescala.y);

    }

    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // Obtenemos la posicion del avatar
        Vector2d avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                stateObs.getAvatarPosition().y / fescala.y);

        // DEBUG
        System.out.println("\n---------------------");
        System.out.println("Posicion del avatar: " + avatar.toString());
        System.out.println("Ultima posicion: " + ultimaPos);

        // Actualizamos el grid
        grid = stateObs.getObservationGrid();

        // Actualizamos el número de gemas que tiene
        if(stateObs.getAvatarResources().isEmpty() != true){
            if(nGemas != stateObs.getAvatarResources().get(6)){
                nGemas++;
                actualizarmapa = true;
                // DEBUG
                System.out.println("Cogemos una gema y actualizamos los caminos");
            }
        }

        // Elimina un paso si ya has llegado a la posición
        if(path != null){  //da un nul pointer cuando no encuentra camino a la gema mas cercana y EXPLOTA
            if (!path.isEmpty()){
                if ((avatar.x != ultimaPos.x) || (avatar.y != ultimaPos.y)){
                    path.remove(0);
                    ticks_stopped = 0;
                    ticks_sin_caminos = 0;
                    objetivo_rocas = false;
                }
                else
                    ticks_stopped++;
            } else {
                actualizarmapa = true;
                System.out.println("Path estaba vacio luego actualizamos");
            }
        } else {
            path = new ArrayList<>();
            actualizarmapa = true;
            System.out.println("Path era null luego actualizamos");
        }

        if(ticks_stopped > 2){
            System.out.println("Llevamos mas de dos ticks parados, luego actualizamos los caminos");
            actualizarmapa = true;
        }

        if(ticks_sin_caminos > 4){
          System.out.println("LLevamos mas de 4 ticks parados, vamos a mover pocas");
          ArrayList<Observation> posiciones_rocas = stateObs.getMovablePositions()[0];
          ArrayList<Vector2d> pos_debajo_rocas = new ArrayList<Vector2d>();

          for(Observation roca : posiciones_rocas){
            pos_debajo_rocas.add( new Vector2d( (int) (roca.position.x / stateObs.getBlockSize()), (int) (roca.position.y / stateObs.getBlockSize()) +1));
          }

          if(pos_debajo_rocas.size() > 0){
            Vector2d pos = pos_debajo_rocas.get(0);
            int x = (int) (pos.x /stateObs.getBlockSize());
            int y = (int) (pos.y /stateObs.getBlockSize());


            System.out.print("Pos x rocas: " + Integer.toString(x));
            System.out.print("Pos y rocas: " + Integer.toString(y));


            Node roca_node = new Node(pos);
            Node avatar_node = new Node(avatar);


            path = pf.astar._findPath(avatar_node, roca_node);
            objetivo_rocas = true;

            if(path == null) System.out.print("\nPues el path de las rocas es nuuuuuuuuuul");

          }


        }

        //Si no hay un plan de ruta calculado...
        if(actualizarmapa && !objetivo_rocas){
            actualizarmapa = false;
            // DEBUG
            System.out.print("\nRecalculando caminos ---------------");

            Node avatar_node = new Node(avatar);

            //Si ya tiene todas las gemas se calcula uno al portal mas cercano. Si no se calcula a la gema mas cercana
            if(nGemas == 10){
                // Actualizamos el grid que contiene el pathfinder
                pf_10_gemas.state = stateObs.copy();
                pf_10_gemas.grid = grid;

                Vector2d portal;

                //Se crea una lista de observaciones de portales, ordenada por cercania al avatar
                ArrayList<Observation>[] posiciones = stateObs.getPortalsPositions(stateObs.getAvatarPosition());

                //Se seleccionan el portal mas cercano
                portal = posiciones[0].get(0).position;

                //Se le aplica el factor de escala para que las coordenas de pixel coincidan con las coordenadas del grid
                portal.x = portal.x / fescala.x;
                portal.y = portal.y / fescala.y;

                Node portal_node = new Node(portal);

                //Calculamos un camino desde la posicion del avatar a la posicion del portal
                path = pf_10_gemas.astar._findPath(avatar_node, portal_node);
            }
            else{

                // Actualizamos el grid que contiene el pathfinder
                pf.state = stateObs.copy();
                pf.grid = grid;

                Boolean hay_path = false;
                int gema_objetivo = 0;

                //Se crea una lista de observaciones, ordenada por cercania al avatar
                ArrayList<Observation> posiciones_gemas = stateObs.getResourcesPositions(stateObs.getAvatarPosition())[0];
                ArrayList<Integer> path_lengths = new ArrayList(posiciones_gemas.size());

                // Almacenamos la longitud de cada camino
                for (Observation obs : posiciones_gemas) {
                    Vector2d gema = obs.position.copy();

                    gema.x = gema.x / fescala.x;
                    gema.y = gema.y / fescala.y;
                    path = pf.getPath(avatar, gema);

                    if(path != null)
                        path_lengths.add(path.size());
                    else
                        path_lengths.add(Integer.MAX_VALUE);
                }

                // Ordena las gemas por longitud menor del camino
                int n = path_lengths.size();
                for (int j = 1; j < n; j++) {
                    int l_key = path_lengths.get(j);
                    Observation o_key = posiciones_gemas.get(j);
                    int i = j-1;
                    while ( (i > -1) && ( path_lengths.get(i) > l_key ) ) {
                        posiciones_gemas.set(i+1, posiciones_gemas.get(i));
                        path_lengths.set(i+1, path_lengths.get(i));
                        i--;
                    }
                    posiciones_gemas.set(i+1, o_key);
                    path_lengths.set(i+1, l_key);
                }

                while (!hay_path && gema_objetivo < posiciones_gemas.size()){
                    Vector2d gema;

                    //Se selecciona la gema mas cercana
                    gema = posiciones_gemas.get(gema_objetivo).position;

                    //Se le aplica el factor de escala para que las coordenas de pixel coincidan con las coordenadas del grig
                    gema.x = gema.x / fescala.x;
                    gema.y = gema.y / fescala.y;

                    Node gema_node = new Node(gema);

                    // DEBUG
                    System.out.print("\nGema siguiente:");

                    System.out.print(Double.toString(gema.x) + ", ");
                    System.out.print(Double.toString(gema.y) + "\n");

                    //Calculamos un camino desde la posicion del avatar a la posicion de la gema
                    path = pf.astar._findPath(avatar_node, gema_node);

                    //Comprobamos si hay camino a dicha gema
                    hay_path = (path != null) && (!path.isEmpty());

                    // DEBUG
                    System.out.println("\nHay camino a la gema " + Integer.toString(gema_objetivo) + "? " + Boolean.toString(hay_path));

                    if (!hay_path) gema_objetivo++;
                }
            }
        }

        if(path == null){
            actualizarmapa = true;
            Types.ACTIONS siguienteaccion = Types.ACTIONS.ACTION_NIL;
            ticks_sin_caminos ++;

            //DEBUG
            System.out.print("\nPath es null.");

            //Si la siguiente accion es peligrosa la cambia sino la deja tal cual
            siguienteaccion = esPeligrosa(stateObs, siguienteaccion);

            return siguienteaccion;
        }

        if(!path.isEmpty()){
            Types.ACTIONS siguienteaccion;
            Node siguientePos = path.get(0);

            //Se determina el siguiente movimiento a partir de la posicion del avatar
            if(siguientePos.position.x != avatar.x){
                if (siguientePos.position.x > avatar.x) {
                    siguienteaccion = Types.ACTIONS.ACTION_RIGHT;
                } else {
                    siguienteaccion = Types.ACTIONS.ACTION_LEFT;
                }
            }else{
                if (siguientePos.position.y > avatar.y){
                    siguienteaccion = Types.ACTIONS.ACTION_DOWN;
                } else {
                    siguienteaccion = Types.ACTIONS.ACTION_UP;
                }
            }

            //Se actualiza la ultima posicion del avatar
            ultimaPos = avatar;

            // DEBUG Muestra siguiente acción, posición actual, etc
            System.out.print("\nSiguiente posicion:");
            System.out.print(Double.toString(siguientePos.position.x) + ", ");
            System.out.print(Double.toString(siguientePos.position.y) + "\n");

            //Si la siguiente accion es peligrosa la cambia sino la deja tal cual
            Types.ACTIONS aux_accion = esPeligrosa(stateObs, siguienteaccion);
            if (aux_accion != siguienteaccion) {
                siguienteaccion = aux_accion;
                actualizarmapa = true;
            }

            // Baja la velocidad para poder ver sus movimientos
            //DEBUG
            try{
                Thread.sleep(50);
            }
            catch(Exception e){}

            //DEBUG
            System.out.print(siguienteaccion.toString());
            //Se devuelve la accion deseada
            return siguienteaccion;
        }
        else{
            //Salida por defecto
            return Types.ACTIONS.ACTION_NIL;
        }

    }

    private Types.ACTIONS esPeligrosa(StateObservation stateObs, Types.ACTIONS siguienteaccion){

        // Array con todas las posibles acciones
        ArrayList<Types.ACTIONS> posibles_acciones = new ArrayList();
        for (int i = 0; i < Types.ACTIONS.values().length; i++){
            posibles_acciones.add(Types.ACTIONS.values()[i]);
        }

        Boolean peligro = false;

        StateObservation aux_stateobs = stateObs.copy();
        aux_stateobs.advance(siguienteaccion);

        // DEBUG
        System.out.println("\nSe muere en la siguiente?");
        System.out.println(Boolean.toString(!aux_stateobs.isAvatarAlive()));

        peligro = !aux_stateobs.isAvatarAlive();

        // Comprobamos que no entra en una situación en la que siempre muere
        if(!peligro){
            boolean muere_siempre = true;
            int ind = 0;
            Types.ACTIONS accion_futura;

            do {
                accion_futura = Types.ACTIONS.values()[ind];
                aux_stateobs = stateObs.copy();
                aux_stateobs.advance(siguienteaccion);
                aux_stateobs.advance(accion_futura);
                ind += 1;
            } while (!aux_stateobs.isAvatarAlive() && ind < Types.ACTIONS.values().length);

            if (aux_stateobs.isAvatarAlive()){
                muere_siempre = false;
            }

            if (!muere_siempre)
                return siguienteaccion;
            else
                posibles_acciones.remove(siguienteaccion);
        }

        for (Types.ACTIONS accion_candidata : posibles_acciones){
            // Comprobamos que no entra en una situación en la que siempre muere
            if (aux_stateobs.isAvatarAlive()){
                boolean muere_siempre = true;
                int ind = 0;
                Types.ACTIONS accion_futura;

                do {
                    accion_futura = Types.ACTIONS.values()[ind];
                    aux_stateobs = stateObs.copy();
                    aux_stateobs.advance(accion_candidata);
                    aux_stateobs.advance(accion_futura);
                    ind += 1;
                } while (!aux_stateobs.isAvatarAlive() && ind < Types.ACTIONS.values().length);

                if (aux_stateobs.isAvatarAlive()){
                    muere_siempre = false;
                }

                if (!muere_siempre)
                    return accion_candidata;
            }
        }

        System.out.print("Se va a morir");
        return Types.ACTIONS.ACTION_NIL;
    }

    private Boolean haybichoscerca(StateObservation stateObs, Types.ACTIONS siguienteaccion){
      StateObservation newStateObs = stateObs.copy();

      Vector2d position = newStateObs.getAvatarPosition();
      int x = (int) (position.x / fescala.x);
      int y = (int) (position.y / fescala.y);

      //newStateObs.advance(siguienteaccion);
      switch (siguienteaccion) {

          case ACTION_LEFT :
              x = x-1;
              break;
          case ACTION_RIGHT :
              x = x+1;
              break;
          case ACTION_UP :
              y = y-1;
              break;
          case ACTION_DOWN :
              y = y+1;
              break;

      }

    for(int i = -1; i <= 1; i++){
      for(int j = -1; j <= 1; j++){
        for(core.game.Observation obs: newStateObs.getObservationGrid()[x+i][y+j])
          if(obs.itype == 10 || obs.itype == 11)
            return true;
      }
    }

      return false;
    }

    private void simularacciones(StateObservation stateObs){
        //Obtenemos la lista de acciones disponible
        ArrayList<Types.ACTIONS> acciones = stateObs.getAvailableActions();

        //Guardamos la informacion sobre el estado inicial
        StateObservation viejoEstado = stateObs;

        for(Types.ACTIONS accion:acciones){
            //avanzamos el estado tras aplicarle una accion
            viejoEstado.advance(accion);

            //viejoEstado.somethingsomething(parametros);  <- Hacemos lo que queramos con el estado avanzado

            //Restauramos el estado para avanzarlo con otra de las acciones disponibles.
            viejoEstado = stateObs;
        }
    }
}
