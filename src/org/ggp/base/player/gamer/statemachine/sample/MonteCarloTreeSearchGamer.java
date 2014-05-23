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

public class MonteCarloTreeSearchGamer extends StateMachineGamer {

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
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	}

	class Node {
	    public int utility;
	    public int visits;
	    public Node parent;
	    public List<Node> children;
	}

	private int limit = 2;
	private long timelimit = 0;
	private long starttime = 0;
	@Override

	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		starttime = System.currentTimeMillis();
		timelimit = timeout-starttime-1000;

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
		notifyObservers(new GamerSelectedMoveEvent(moves, thisMove, stop - starttime));
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

	private Node select(Node node) {
		if(node.visits == 0) {
			return node;
		}
		for(int i = 0; i < node.children.size(); i++) {
			if(node.children.get(i).visits == 0) {
				return node.children.get(i);
			}
		}
		double score = 0;
		Node result = node;
		for(int i = 0; i < node.children.size(); i++) {
			double newScore = selectfn(node.children.get(i));
			if(newScore > score) {
				score = newScore;
				result = node.children.get(i);
			}
 		}
		return select(result);
	}

	private double selectfn(Node node) {
		return node.utility + Math.sqrt(2*Math.log(node.parent.visits)/node.visits);
	}

	private boolean backpropagate(Node node, int score) {
		node.visits = node.visits+1;
		node.utility = node.utility+score;
		if (node.parent != null) { backpropagate(node.parent,score); }
		return true;
	}

}
