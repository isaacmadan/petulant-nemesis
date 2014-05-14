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

public final class MonteCarloSearchGamer extends StateMachineGamer {

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

	class ScoreMove {
	    public int score;
	    public Move move;
	}

	public ScoreMove minimaxMove(MachineState state, int alpha, int beta, int depth) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		Role role = getRole();
		if (getStateMachine().isTerminal(state)) {
			ScoreMove sm = new ScoreMove();
			sm.score = getStateMachine().getGoal(state, role);
			return sm;
//			return getStateMachine().getGoal(state, role);
		}

		Move bestMove = null;
		int bestScore = alpha;
//		int movesLookedAt = 0;
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		for(Move move : moves) {
			List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(state, role, move);
			int worstScore = beta;
			for(List<Move> jointMove : jointMoves) {
				MachineState nextState = getStateMachine().getNextState(state, jointMove);
				ScoreMove sm = minimaxMove(nextState, bestScore, worstScore, depth+1);
//				int value = minimaxMove(nextState, bestScore, worstScore, depth+1);
				int value = sm.score;
				worstScore = Math.min(worstScore, value);
				if(worstScore <= bestScore) {
					worstScore = bestScore;
					break;
				}
			}

			if(bestScore >= beta) {
				ScoreMove sm = new ScoreMove();
				sm.move = move;
				sm.score = worstScore;
				return sm;
//				return worstScore;
			}
			if(bestScore < worstScore) {
				bestScore = worstScore;
				bestMove = move;
			}
		}
		ScoreMove sm = new ScoreMove();
		sm.score = bestScore;
		sm.move = bestMove;
		return sm;
//		return bestScore;
	}

	private int limit = 4;

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		if(moves.size() == 1) {
			return moves.get(0);
		}
		ScoreMove thisScoreMove = minimaxMove(getCurrentState(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
		Move thisMove = thisScoreMove.move;
		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, thisMove, stop - start));
		return thisMove;
	}

	/**
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();

		double score = Integer.MIN_VALUE;
		Move thisMove = null;
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		for (Move move : moves) {
			double result = minScore(getCurrentState(), getRole(), move, 0);
			if (result > score) {
				score = result;
				thisMove = move;
			}
		}

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, thisMove, stop - start));

		return thisMove;
	}
	**/

	private double maxScore(MachineState state, Role role, int level) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, role);
		}
		if(level >= limit) return monteCarlo(role,state,4);
		double value = Integer.MIN_VALUE;
		List<Move> moves = getStateMachine().getLegalMoves(state, getRole());
		for (Move move : moves) {
			value = Math.max(value, minScore(state, role, move, level));
		}
		return value;
	}

	private double minScore(MachineState state, Role role, Move move, int level) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		double value = Integer.MAX_VALUE;
		List<List<Move>> moves = getStateMachine().getLegalJointMoves(state, role, move);
		for (List<Move> jointMove : moves) {
			MachineState nextState = getStateMachine().getNextState(state, jointMove);
			value = Math.min(value, maxScore(nextState, role, level+1));
		}
		return value;
	}

	private double monteCarlo(Role role, MachineState state, int count) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		int total = 0;
		for(int i = 0; i < count; i++) {
			total = total + depthCharge(role, state);
		}
		return total/count;
	}

	private int depthCharge(Role role, MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, role);
		}
		List<List<Move>> moves = getStateMachine().getLegalJointMoves(state);
		int index = (int)(Math.random() * moves.size());
		List<Move> randomList = moves.get(index);
		MachineState randomNextState = getStateMachine().getNextState(state, randomList);
		return depthCharge(role, randomNextState);
	}
}
