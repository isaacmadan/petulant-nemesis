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

public final class NewerMonteCarloSearchGamer extends StateMachineGamer {

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
	private long timelimit = 0;
	private long starttime = 0;

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		starttime = System.currentTimeMillis();
		timelimit = timeout-start-1000;
		System.out.println(timelimit);

		double score = Integer.MIN_VALUE;
		Move thisMove = null;
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		for (Move move : moves) {
			double result = minScore(getCurrentState(), getRole(), move, 0);
			if (result > score) {
				score = result;
				thisMove = move;
			}

			if(System.currentTimeMillis()-starttime > timelimit) break;
		}

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, thisMove, stop - start));

		return thisMove;
	}

	private double maxScore(MachineState state, Role role, int level) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, role);
		}
		if(level >= limit) return monteCarlo(role, state, 6);
		double value = Integer.MIN_VALUE;
		List<Move> moves = getStateMachine().getLegalMoves(state, getRole());
		for (Move move : moves) {
			value = Math.max(value, minScore(state, role, move, level));
			if(System.currentTimeMillis()-starttime > timelimit) break;
		}
		return value;
	}

	private double minScore(MachineState state, Role role, Move move, int level) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		double value = Integer.MAX_VALUE;
		List<List<Move>> moves = getStateMachine().getLegalJointMoves(state, role, move);
		for (List<Move> jointMove : moves) {
			MachineState nextState = getStateMachine().getNextState(state, jointMove);
			value = Math.min(value, maxScore(nextState, role, level+1));
			if(System.currentTimeMillis()-starttime > timelimit) break;
		}
		return value;
	}

	private double monteCarlo(Role role, MachineState state, int count) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		double total = 0;
		for(int i=0; i < count; i++) {
			total = total + depthCharge(role, state);
			if(System.currentTimeMillis()-starttime > timelimit) break;
		}
		return total/count;
	}

	private double depthCharge(Role role, MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, role);
		}

		List<List <Move>> jointMoves = getStateMachine().getLegalJointMoves(state);
		int rand = (int)(Math.random() * jointMoves.size());
		List<Move> randomJointMove = jointMoves.get(rand);

		MachineState nextState = getStateMachine().getNextState(state, randomJointMove);
		return depthCharge(role, nextState);
	}

}
