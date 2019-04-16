package practica_busqueda;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tools.pathfinder.Node;
import tools.pathfinder.PathFinder;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Agent extends BaseAgent {
    //Objeto de clase Pathfinder
    private PathFinder pf;
    private Vector2d fescala;
    private ArrayList<Node> path  = new ArrayList<>();
    private Vector2d ultimaPos;
    private Boolean actualizarmapa = false;

    private int nGemas = 0;

    private ArrayList<Observation>[][] grid;
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

        /*
           if(path == null){
           posiciones[0].remove(0);

           return Types.ACTIONS.ACTION_LEFT;
           }
           */

        // Elimina un paso si ya has llegado a la posición
        if(path != null) //da un nul pointer cuando no encuentra camino a la gema mas cercana y EXPLOTA
            if (!path.isEmpty()){
                if ((avatar.x != ultimaPos.x) || (avatar.y != ultimaPos.y))
                    path.remove(0);
            }

        if(actualizarmapa){
            path.clear();
            actualizarmapa = false;
        }

        //Si no hay un plan de ruta calculado...
        if(path.isEmpty()){
            System.out.print("\nNo hay path");

            // Actualizamos el grid que contiene el pathfinder
            pf.state = stateObs.copy();
            pf.grid = grid;
            // Actualizamos los caminos a partir de nuestra posición
            pf.runAll((int) avatar.x, (int) avatar.y);

            System.out.println(grid.length);
            System.out.println(grid[0].length);
            for(int i = 0; i < grid.length; ++i){
                System.out.print("\n");
                for(int j = 0; j < grid[i].length; ++j){
                    if(!grid[i][j].isEmpty())
                        System.out.print(Integer.toString(grid[i][j].get(0).itype) + "\t");
                    else
                        System.out.print("," + "\t");
                }
            }

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
                Vector2d gema;

                //Se crea una lista de observaciones, ordenada por cercania al avatar
                ArrayList<Observation>[] posiciones = stateObs.getResourcesPositions(stateObs.getAvatarPosition());

                //Se selecciona la gema mas cercana
                if(path == null){
                    gema = posiciones[0].get(1).position;
                    System.out.print("a por otra gema");
                }
                else
                    gema = posiciones[0].get(0).position;

                //Se le aplica el factor de escala para que las coordenas de pixel coincidan con las coordenadas del grig
                gema.x = gema.x / fescala.x;
                gema.y = gema.y / fescala.y;

                System.out.print("Gema siguiente:");
                System.out.print(gema.x);
                System.out.print(gema.y);

                //Calculamos un camino desde la posicion del avatar a la posicion de la gema
                path = pf.getPath(avatar, gema);

            }
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

            int p = esPeligrosa(grid, ultimaPos);
            if(p > 0){
                System.out.print("\nPELIGROOOOOOOOOOO");
                if(p == 1){
                    siguienteaccion = Types.ACTIONS.ACTION_RIGHT;
                }

                if(p == 2){
                    siguienteaccion = Types.ACTIONS.ACTION_LEFT;
                }

            }

            //Si no se puede mover porque hay una piedra actualiza el mapa
            actualizarmapa = puedoMoverme(grid, ultimaPos, siguienteaccion);

            // Baja la velocidad para poder ver sus movimientos
            try{
                Thread.sleep(50);
            }
            catch(Exception e){}

            //Se devuelve la accion deseada
            return siguienteaccion;
        }
        else{
            //Salida por defecto
            return Types.ACTIONS.ACTION_NIL;
        }

    }

    private int esPeligrosa(ArrayList<Observation>[][] grid, Vector2d siguientePos){

        int peligro = 0;

        int x = (int) siguientePos.x;
        int y = (int) siguientePos.y;

        if (y-2 > 0){
            ArrayList<Observation> obs1 = grid[x][y-1];
            ArrayList<Observation> obs2 = grid[x][y-2];

            /*
               for (int i=0; i < obs1.size() ;i++ ) {
               System.out.print(obs1.get(i).toString());
               }
               */

            if (obs1.size() > 0){
                // Si el objeto de encima es una PIEDRA
                if ((obs1.get(0).itype == 7)){
                    System.out.print("Tiene la roca encima");
                    // TODO Preguntarle a johanna
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

        return peligro;
    }

    /*
       Mira a ver si el machanguito se puede mover o hay una piedra
       */

    private Boolean puedoMoverme(ArrayList<Observation>[][] grid, Vector2d posicion, Types.ACTIONS siguiente){
        Boolean puedomoverme = false;
        ArrayList<Observation> obs;

        int x = (int) posicion.x;
        int y = (int) posicion.y;

        if(siguiente == Types.ACTIONS.ACTION_LEFT)
            obs = grid[x-1][y];
        else
            obs = grid[x+1][y];

        if(obs.size() > 0)
            if(obs.get(0).itype == 7)
                puedomoverme = true;

        return puedomoverme;
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
