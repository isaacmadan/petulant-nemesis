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

	class Node extends MachineState{
	    public int utility;
	    public int visits;
	    public Node parent;
	    public List<Node> children;
	    public MachineState state;

	    public Node(MachineState state) {
	    	this.state = state;
	    	utility = 0;
	    	visits = 0;
	    	parent = null;
	    	children = new ArrayList<Node>();
	    }

	    public void addChild(Node node) {
	    	children.add(node);
	    }
	}

	class Tree {
		public Node root;

		public Tree(Node root) {
			this.root = root;
		}
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
		MachineState currentState = getCurrentState();
		Node node = new Node(currentState);
		node.visits = 1;
		Tree tree = new Tree(node);
		StateMachine stateMachine = getStateMachine();
		Role myRole = getRole();

		List<Move> moves = stateMachine.getLegalMoves(currentState, myRole);
		for (Move move : moves) {
			List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(currentState, myRole, move);
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = getStateMachine().getNextState(currentState, jointMove);
				node.addChild(new Node(nextState));
			}
		}

		Node chosen = select(node);
		expand(chosen, myRole, stateMachine);

		List<Move> chosenMoves = stateMachine.getLegalMoves(chosen.state, myRole);
		for (Move move : chosenMoves) {
			double result = minScore(chosen.state, myRole, move, 0);
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

	private Node select(Node node)  throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if(node.visits == 0) return node;
		for(int i = 0; i < node.children.size(); i++) {
			if(node.children.get(i).visits == 0) return node.children.get(i);
		}
		double score = 0;
		Node result = node;
		for(int i = 0; i < node.children.size(); i++) {
			double newscore = selectfn(node.children.get(i));
			if(newscore > score) {
				score = newscore;
				result = node.children.get(i);
			}
		}
		return select(result);
	}

	private double selectfn(Node node) {
		return node.utility + Math.sqrt(2*Math.log(node.parent.visits)/node.visits);
	}

	private void expand(Node chosen, Role myRole, StateMachine stateMachine) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		List<Move> moves = stateMachine.getLegalMoves(chosen.state, myRole);
		for (Move move : moves) {
			List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(chosen.state, myRole, move);
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = getStateMachine().getNextState(chosen.state, jointMove);
				chosen.addChild(new Node(nextState));
			}
		}
	}

	private boolean backpropagate(Node node, int score) {
		node.visits = node.visits+1;
		node.utility = node.utility+score;
		if (node.parent != null) { backpropagate(node.parent,score); }
		return true;
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
