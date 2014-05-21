package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
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

public class NewMonteCarloSearchGamer extends StateMachineGamer {

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

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		if(moves.size() == 1) {
			return moves.get(0);
		}

		Move bestMove = moves.get(0);
		double bestScore = 0;

		for(Move move : moves) {
			List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(getCurrentState(), getRole(), move);
			for(List<Move> jointMove : jointMoves) {
				double score = monteCarlo(jointMove);
				if(score > bestScore) {
					bestMove = move;
					bestScore = score;
				}
			}
		}

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, bestMove, stop - start));
		return bestMove;
	}

	public double monteCarlo(List<Move> jointMove) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if (getStateMachine().isTerminal(getCurrentState())) {
			return getStateMachine().getGoal(getCurrentState(), getRole());
		}

		MachineState nextState = getStateMachine().getNextState(getCurrentState(), jointMove);
		List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(nextState);

		List<List<Move>> randomJointMoves = new ArrayList();
		for(int i=0; i< 4; i++) {
			int rand = (int)(Math.random() * jointMoves.size());
			List<Move> obj = jointMoves.remove(rand);
			randomJointMoves.add(obj);
		}

		int result = 0;
		for(List<Move> randomJointMove : randomJointMoves) {
			result += depth(randomJointMove, nextState);
		}

		return result/4;
	}

	public int depth(List<Move> jointMove, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if (getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, getRole());
		}

		MachineState nextState = getStateMachine().getNextState(state, jointMove);
		List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(nextState);
		int rand = (int)(Math.random() * jointMoves.size());
		List<Move> randomJointMove = jointMoves.get(rand);
		return depth(randomJointMove, nextState);
	}
}
