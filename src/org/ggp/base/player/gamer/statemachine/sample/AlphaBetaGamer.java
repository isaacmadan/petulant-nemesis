package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public final class AlphaBetaGamer extends StateMachineGamer {

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
	public void stateMachineStop() {}

	@Override
	public void stateMachineAbort() {}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {}

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

		int score = 0;
		int alpha = -1;
		int beta = 100;
		Move action = moves.get(0);

		for (int i = 0; i < moves.size(); i++) {
			int result = minScore(moves.get(i), getCurrentState(), alpha, beta);
			if (result > score) {
				score = result;
				action = moves.get(i);
			}
		}

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, action, stop - start));
		return action;
	}

	public int maxScore(MachineState state, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if(getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, getRole());
		}

		List<Move> moves = getStateMachine().getLegalMoves(state, getRole());
		for(int i = 0; i < moves.size(); i++) {
			int result = minScore(moves.get(i), getCurrentState(), alpha, beta);
			alpha = Math.max(alpha, result);
			if(alpha >= beta) {
				return beta;
			}
		}

		return alpha;
	}

	public int minScore(Move move, MachineState state, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(state, getRole(), move);
		List<Move> opponentMoves = new ArrayList<Move>();
		for(List<Move> moveList : jointMoves) {
			if(moveList.size() > 1) {
				Move opponentMove = moveList.get(1);
				if(!opponentMoves.contains(opponentMove)) {
					opponentMoves.add(opponentMove);
				}
			}
		}

//		List<Role> roles = getStateMachine().getRoles();
//		Map<Role, Integer>roleIndexMap = getStateMachine().getRoleIndices();
//		Integer myIndex = roleIndexMap.get(getRole());
//		Role opponent = roles.get(myIndex++ < roles.size() - 1? myIndex : 0);
//		List<Move> moves = getStateMachine().getLegalMoves(state, opponent);
//		for(Move m : moves) {
//			MachineState nextState = getStateMachine().getNextState(state, moves);
//			int result = maxScore(nextState, alpha, beta);
//			beta = Math.min(beta, result);
//			if(beta <= alpha) {
//				return alpha;
//			}
//		}

//		for(List<Move> moveList : jointMoves) {
//			MachineState nextState = getStateMachine().getNextState(state, moveList);
//			int result = maxScore(nextState, alpha, beta);
//			beta = Math.min(beta,  result);
//			if(beta <= alpha) {
//				return alpha;
//			}
//		}

		for(int i = 0; i < opponentMoves.size(); i++) {
			Move mv = opponentMoves.get(i);
			List<Move> mvs = Arrays.asList(mv);
			MachineState nextState = getStateMachine().getNextState(state, mvs);
			int result = maxScore(nextState, alpha, beta);
			beta = Math.min(beta, result);
			if(beta <= alpha) {
				return alpha;
			}
		}

		return beta;
	}
}
