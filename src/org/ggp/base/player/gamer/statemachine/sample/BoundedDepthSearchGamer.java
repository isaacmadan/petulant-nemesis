package org.ggp.base.player.gamer.statemachine.sample;

import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public final class BoundedDepthSearchGamer extends StateMachineGamer {

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}

	@Override
	public void stateMachineStop() {
	}

	@Override
	public void stateMachineAbort() {
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
	}

	private Role me;
	private Role opponent;
	private int limit = 2;

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		long start = System.currentTimeMillis();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(),
				getRole());

		int score = 0;
		Move action = moves.get(0);

		for (int i = 0; i < moves.size(); i++) {
			int result = minScore(moves.get(i), getCurrentState(), 0);
			if (result > score) {
				score = result;
				action = moves.get(i);
			}
		}

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, action, stop - start));
		return action;
	}

	public int minScore(Move move, MachineState state, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		int badScore = 100;
		List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(state, getRole(), move);

		for(List<Move>moveList : jointMoves) {
			int goodScore = -1;
			MachineState nextState = getStateMachine().getNextState(state, moveList);
			level++;
			if(getStateMachine().isTerminal(nextState)) {
				int goal = getStateMachine().getGoal(nextState, getRole());
				goodScore = goal;
				if(level >= limit) { goodScore = 0; }
			} else {
				List<Move> moves = getStateMachine().getLegalMoves(nextState, getRole());
				for(int i = 0; i < moves.size(); i++) {
					int result = minScore(moves.get(i), nextState, level);
					goodScore = Math.max(goodScore, result);
					if(goodScore == 100) {
						break;
					}
				}
			}
			badScore = Math.min(badScore, goodScore);
			if(badScore == 0) {
				break;
			}
		}
		return badScore;
	}

//	public int minScore(Move move, MachineState state, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
//		List<Move> opponentMoves = getStateMachine().getLegalMoves(getCurrentState(), opponent);
//		int score = 100;
//
//		for(int i=0; i< opponentMoves.size(); i++) {
//			List<Move> moveAttempts = new ArrayList<Move>();
//			for(Map.Entry<Role, Integer> entry : getStateMachine().getRoleIndices().entrySet()){
//				if(entry.getKey().equals(me)) {
//					moveAttempts.add(move);
//				} else {
//					moveAttempts.add(opponentMoves.get(i));
//				}
//			}
//
//			MachineState nextState = getStateMachine().getNextState(state, moveAttempts);
//			int result = maxScore(nextState, level+1);
//			if(result == 0) return 0;
//			if(result < score) score = result;
//		}
//		return score;
//	}
//
//	public int maxScore(MachineState state, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
//		if(getStateMachine().isTerminal(state)) {
//			return getStateMachine().getGoal(state, me);
//		}
//		if(level >= limit) return 0;
//		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), me);
//		int score = 0;
//		for(int i=0; i<moves.size(); i++) {
//			int result = minScore(moves.get(i), state, level);
//			if(result == 100) return 100;
//			if(result > score) score = result;
//		}
//		return score;
//	}
}
