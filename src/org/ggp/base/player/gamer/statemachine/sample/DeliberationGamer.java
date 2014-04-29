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
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public final class DeliberationGamer extends StateMachineGamer {

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
		Move action = moves.get(0);

		for(int i = 0; i < moves.size(); i++) {
			int result = maxScore(moves.get(i), getStateMachine().getNextState(getCurrentState(), moves.subList(i,i+1)));
			if(result == 100) {
				return moves.get(i);
			}
			if(result > score) {
				score = result;
				action = moves.get(i);
			}
		}

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, action, stop - start));
		return action;
	}

	public int maxScore(Move move, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if(getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, getRole());
		}

		int score = 0;
		List<Move> moves = getStateMachine().getLegalMoves(state, getRole());
		for(int i = 0; i < moves.size(); i++) {
			int result = maxScore(moves.get(i), getStateMachine().getNextState(state, moves.subList(i,i+1)));
			if(result > score) {
				score = result;
			}
		}

		return score;
	}
}
