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
    private PathFinder pf;
    private ArrayList<Node> path  = new ArrayList<>();
    private ArrayList<Observation>[][] grid;

    private Vector2d ultimaPos;

    // Auxiliar variables
    private Vector2d fescala;
    private Boolean actualizarmapa = false;
    private int nGemas = 0;
    private int ticks_stopped = 0;

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
                System.out.println("Cogemos una gema y actualizamos los caminos");
            }
        }

        // Elimina un paso si ya has llegado a la posición
        if(path != null) //da un nul pointer cuando no encuentra camino a la gema mas cercana y EXPLOTA
            if (!path.isEmpty()){
                if ((avatar.x != ultimaPos.x) || (avatar.y != ultimaPos.y)){
                    path.remove(0);
                    ticks_stopped = 0;
                }
                else
                    ticks_stopped++;
            }

        if(ticks_stopped > 2){
            actualizarmapa = true;
        }

        if(actualizarmapa){
            if(path != null)
                path.clear();
            else
                path = new ArrayList<>();

            actualizarmapa = false;
        }

        //Si no hay un plan de ruta calculado...
        if(path != null)
            if(path.isEmpty()){
                // DEBUG
                System.out.print("\nNo hay camino, lo recalculamos ---------------");

                // Actualizamos el grid que contiene el pathfinder
                pf.state = stateObs.copy();
                pf.grid = grid;
                // Actualizamos los caminos a partir de nuestra posición
                pf.runAll((int) avatar.x, (int) avatar.y);

                //Si ya tiene todas las gemas se calcula uno al portal mas cercano. Si no se calcula a la gema mas cercana
                if(nGemas == 10){
                    Vector2d portal;

                    //Se crea una lista de observaciones de portales, ordenada por cercania al avatar
                    ArrayList<Observation>[] posiciones = stateObs.getPortalsPositions(stateObs.getAvatarPosition());

                    //Se seleccionan el portal mas cercano
                    portal = posiciones[0].get(0).position;

                    //Se le aplica el factor de escala para que las coordenas de pixel coincidan con las coordenadas del grid
                    portal.x = portal.x / fescala.x;
                    portal.y = portal.y / fescala.y;

                    //Calculamos un camino desde la posicion del avatar a la posicion del portal
                    path = pf.getPath(avatar, portal);
                }
                else{
                    Boolean hay_path = false;
                    int gema_objetivo = 0;

                    //Se crea una lista de observaciones, ordenada por cercania al avatar
                    ArrayList<Observation>[] posiciones = stateObs.getResourcesPositions(stateObs.getAvatarPosition());

                    // NOTE: No podemos comprobar que no se salga de rango porque posiciones es un array
                    // luego no podemos saber su longitud
                    while (!hay_path){
                        Vector2d gema;

                        //Se selecciona la gema mas cercana
                        gema = posiciones[0].get(gema_objetivo).position;

                        //Se le aplica el factor de escala para que las coordenas de pixel coincidan con las coordenadas del grig
                        gema.x = gema.x / fescala.x;
                        gema.y = gema.y / fescala.y;

                        // DEBUG
                        System.out.print("\nGema siguiente:");

                        System.out.print(Double.toString(gema.x) + ", ");
                        System.out.print(Double.toString(gema.y) + "\n");

                        //Calculamos un camino desde la posicion del avatar a la posicion de la gema
                        path = pf.getPath(avatar, gema);

                        //Comprobamos si hay camino a dicha gema
                        hay_path = path != null;
                        if (!hay_path) gema_objetivo++;

                        // DEBUG
                        System.out.println("\nHay camino a la gema " + Integer.toString(gema_objetivo) + "? " + Boolean.toString(hay_path));
                    }
                }
            }

        if(path == null){
            actualizarmapa = true;
            Types.ACTIONS siguienteaccion = Types.ACTIONS.ACTION_NIL;

            //DEBUG
            System.out.print("\nPath es null.");

            /*
               int p = esPeligrosa(grid, ultimaPos, siguienteaccion);
               if(p > 0){
               System.out.print("\nPELIGROOOOOOOOOOO");
               if(p == 1){
               siguienteaccion = Types.ACTIONS.ACTION_RIGHT;
               }

               if(p == 2){
               siguienteaccion = Types.ACTIONS.ACTION_LEFT;
               }
               }
               */
            //Si la siguiente accion es peligrosa la cambia sino la deja tal cual
            siguienteaccion = esPeligrosa(stateObs,siguienteaccion);

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
            siguienteaccion = esPeligrosa(stateObs,siguienteaccion);

            /*
               int p = esPeligrosa(grid, ultimaPos, siguienteaccion);
               if(p > 0){
               System.out.print("\nPELIGROOOOOOOOOOO");
               if(p == 1){
               siguienteaccion = Types.ACTIONS.ACTION_RIGHT;
               }

               if(p == 2){
               siguienteaccion = Types.ACTIONS.ACTION_LEFT;
               }

               }
               */

            // Baja la velocidad para poder ver sus movimientos
            try{
                Thread.sleep(150);
            }
            catch(Exception e){}

            //DEBUG
            switch (siguienteaccion) {

                case ACTION_LEFT :
                    System.out.print("\nDerecha.");
                    break;
                case ACTION_RIGHT :
                    System.out.print("\nIquierda");
                    break;
                case ACTION_UP :
                    System.out.print("\nArriba");
                    break;
                case ACTION_DOWN :
                    System.out.print("\nAbajo");
                    break;

            }

            //Se devuelve la accion deseada
            return siguienteaccion;
        }
        else{
            //Salida por defecto
            return Types.ACTIONS.ACTION_NIL;
        }

    }

    private Types.ACTIONS esPeligrosa(StateObservation stateObs, Types.ACTIONS siguienteaccion){

        Boolean peligro = false;

        StateObservation aux_stateobs = stateObs.copy();
        aux_stateobs.advance(siguienteaccion);

        Vector2d posicion = aux_stateobs.getAvatarPosition();
        int x = (int) posicion.x / aux_stateobs.getBlockSize();
        int y = (int) posicion.y / aux_stateobs.getBlockSize();

        //DEBUG
        System.out.print(x); //POSICION 00 EN EL MAPA 3!!! COMO ES ESTO POSIBLE=????
        System.out.print(y);

        for(core.game.Observation obs: aux_stateobs.getObservationGrid()[x][y])
            if(obs.itype == 7 || obs.itype == 10 || obs.itype == 11)
                peligro = true;

        System.out.println("\nSe muere en la siguiente?");
        System.out.println(Boolean.toString(!aux_stateobs.isAvatarAlive()));

        peligro = !aux_stateobs.isAvatarAlive();

        if(!peligro){
            return siguienteaccion;
        }else{

            if (x-1 >= 0){
                ArrayList<Observation> obsl1 = grid[x-1][y];

                //DEBUG
                System.out.print("\nVeamos que posicion es la mas segura.");

                if ((obsl1.size() > 0)){
                    if ((obsl1.get(0).itype == 10) ||(obsl1.get(0).itype == 11)){
                        siguienteaccion = Types.ACTIONS.ACTION_RIGHT;
                        //DEBUG
                        System.out.print("\nMe muevo a la derecha.");
                    } else{
                        // Siguiente acción: IZQ
                        siguienteaccion = Types.ACTIONS.ACTION_LEFT;
                        //DEBUG
                        System.out.print("\nMe muevo a la derecha.");

                    }
                }
            }
            return siguienteaccion;

        }

        /*
           int x = (int) ultimaPos.x;
           int y = (int) ultimaPos.y;
           */

        /*

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
           */

        /*

           if (y-2 > 0){
           ArrayList<Observation> obs1 = grid[x][y-1];
           ArrayList<Observation> obs2 = grid[x][y-2];

        /*
        for (int i=0; i < obs1.size() ;i++ ) {
        System.out.print(obs1.get(i).toString());
        }
        */

        /*
           if (obs1.size() > 0){
        // Si el objeto de encima es una PIEDRA
        if ((obs1.get(0).itype == 7)){
        System.out.print("Tiene la roca encima");
        if (x-1 >= 0){
        ArrayList<Observation> obsl1 = grid[x-1][y-1];
        ArrayList<Observation> obsl2 = grid[x-1][y-2];

        if ((obsl1.size() > 0) && (obsl2.size() > 0)){
        if ((obsl1.get(0).itype == 10) ||(obsl2.get(0).itype == 10)){
        // Siguiente acción: DER
        peligro = 1;
        } else{
        // Siguiente acción: IZQ
        peligro = 2;
        }
        }

        }

        }
           }

           }

        /*
        for(int i=0; i < grid.length; i++){
        for(int j=0; j < grid[0].length; j++){
        System.out.print(grid[i][j].toString());
        }
        }
        */

        //  return peligro;
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
