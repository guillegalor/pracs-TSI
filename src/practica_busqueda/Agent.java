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

public class Agent extends AbstractPlayer {
    //Objeto de clase Pathfinder
    private PathFinder pf;
    private Vector2d fescala;
    private ArrayList<Node> path  = new ArrayList<>();
    private Vector2d ultimaPos;
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
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

        esPeligrosa(stateObs, ultimaPos);
    }

    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        //Obtenemos la posicion del avatar
        Vector2d avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x, stateObs.getAvatarPosition().y / fescala.y);
        System.out.println("Posicion del avatar: " + avatar.toString());
        System.out.println("Ultima posicion: " + ultimaPos);

        //actualizamos el plan de ruta
        if(((avatar.x != ultimaPos.x) || (avatar.y != ultimaPos.y)) && !path.isEmpty()){
            path.remove(0);
        }

        //Calculamos el numero de gemas que lleva encima
        int nGemas = 0;
        if(stateObs.getAvatarResources().isEmpty() != true){
            nGemas = stateObs.getAvatarResources().get(6);
        }

        //Si no hay un plan de ruta calculado...
        if(path.isEmpty()){
            //Si ya tiene todas las gemas se calcula uno al portal mas cercano. Si no se calcula a la gema mas cercana
            if(nGemas == 10){
                Vector2d portal;

                //Se crea una lista de observaciones de portales, ordenada por cercania al avatar
                ArrayList<Observation>[] posiciones = stateObs.getPortalsPositions(stateObs.getAvatarPosition());

                //Se seleccionan el portal mas cercano
                portal = posiciones[0].get(0).position;

                //Se le aplica el factor de escala para que las coordenas de pixel coincidan con las coordenadas del grig
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
                gema = posiciones[0].get(0).position;

                //Se le aplica el factor de escala para que las coordenas de pixel coincidan con las coordenadas del grig
                gema.x = gema.x / fescala.x;
                gema.y = gema.y / fescala.y;

                //Calculamos un camino desde la posicion del avatar a la posicion de la gema
                path = pf.getPath(avatar, gema);
            }
        }

        if(path != null){
            Types.ACTIONS siguienteaccion;
            Node siguientePos = path.get(0);

            //Se determina el siguiente movimiento a partir de la posicion del avatar
            if(siguientePos.position.x != avatar.x){
                if (siguientePos.position.x > avatar.x) {
                    siguienteaccion = Types.ACTIONS.ACTION_RIGHT;
                }else{
                    siguienteaccion = Types.ACTIONS.ACTION_LEFT;
                }
            }else{
                if(siguientePos.position.y > avatar.y){
                    siguienteaccion = Types.ACTIONS.ACTION_DOWN;
                }else{
                    siguienteaccion = Types.ACTIONS.ACTION_UP;
                }
            }

            //Se actualiza la ultima posicion del avatar
            ultimaPos = avatar;

            // DEBUG Muestra siguiente acción, posición actual, etc
            System.out.print("Siguiente posicion:");
            System.out.print(Double.toString(siguientePos.position.x) + ", ");
            System.out.print(Double.toString(siguientePos.position.y) + "\n");

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

    private Boolean esPeligrosa(StateObservation stateObs, Vector2d siguientePos){
        ArrayList<Observation>[][] grid = stateObs.getObservationGrid();
        Boolean ground_found = false;
        // while (!ground_found){
        // }

        return true;
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
