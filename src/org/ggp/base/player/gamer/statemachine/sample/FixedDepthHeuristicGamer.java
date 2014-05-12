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

public final class FixedDepthHeuristicGamer extends StateMachineGamer {

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

	private int limit = 2;

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();

		int score = Integer.MIN_VALUE;
		Move thisMove = null;
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		for (Move move : moves) {
			int result = minScore(getCurrentState(), getRole(), move, 0);
			if (result > score) {
				score = result;
				thisMove = move;
			}
		}

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, thisMove, stop - start));

		return thisMove;
	}

	private int maxScore(MachineState state, Role role, int level) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, role);
		}
		if(level >= limit) return evalfn(role,state);
		int value = Integer.MIN_VALUE;
		List<Move> moves = getStateMachine().getLegalMoves(state, getRole());
		for (Move move : moves) {
			value = Math.max(value, minScore(state, role, move, level));
		}
		return value;
	}

	private int minScore(MachineState state, Role role, Move move, int level) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		int value = Integer.MAX_VALUE;
		List<List<Move>> moves = getStateMachine().getLegalJointMoves(state, role, move);
		for (List<Move> jointMove : moves) {
			MachineState nextState = getStateMachine().getNextState(state, jointMove);
			value = Math.min(value, maxScore(nextState, role, level+1));
		}
		return value;
	}

	private int evalfn(Role role, MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		return getProximity(role,state);
	}

	private int mobility(Role role, MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, getRole());
		return Math.min(legalMoves.size(), 100);
	}

	private int focus(Role role, MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		return 100 - mobility(role, state);
	}

	private int getProximity(Role role, MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		return getStateMachine().getGoal(state, role);
	}
}
