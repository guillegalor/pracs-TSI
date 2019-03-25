package practica_busqueda;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.Random;

public class Agent extends BaseAgent{
    private ArrayList<Types.ACTIONS> lista_acciones; // Conjunto de acciones posibles

    public Agent (StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        ArrayList<Observation>[] obstacles = stateObs.getImmovablePositions();

        ArrayList<Integer> tiposObs = new ArrayList<Integer>();

        for (ArrayList<Observation>; ; ) {

        }
    }

    public Types.ACTIONS act (StateObservation stateObs , ElapsedCpuTimer elapsedTimer){
      Vector2d avatar = new Vector2d(stateObs.getavatarPosition().x / fescala.x , stateObs.getavatarPosition().y / fescala.y);

      if((( avatar.x != ultimaPos.x ) || (avatar.y != ultimaPos.y)) && !path.isEmpty()){
          path.remove(0);
      }

      int nGemas = 0;

      if(stateObs.getAvatarResources().isEmpty() != true){
          nGemas = stateObs.getAvatarResources().get(6);
      }

      if(path.isEmpty()){

        if(nGemas == 10){
          Vector2d portal;

          ArrayList<Observation>[] posiciones = stateObs.getPortalsPositions(stateObs.getAvatarPosition());

          portal = posiciones[0].get(0).position;

          portal.x = portal.x / fescala.x;
          portal.y = portal.y / fescala.y;

          path = pf.getPath(avatar, portal);

        }else{
          Vector2d gema;

          ArrayList<Observation>[] posiciones = stateObs.getResourcesPositions(stateObs.getAvatarPosition());

          gema = posiciones[0].get(0).position ;

          gema.x = gema.x / fescala.x;
          gema.y = gema.y / fescala.y;

          path = pf.getPath(avatar, gema);
        }
      }

      if(path != null){
        Types.ACTIONS siguienteaccion;
        Node siguientePos = path.get(0);

        if(siguientePos.position.x != avatar.x){
          if(siguientePos.position.x > avatar.x){
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

        ultimaPos = avatar;

        return siguienteaccion;
      }else{
        return Types.ACTIONS.ACTION_NIL;
      }


    }
}
