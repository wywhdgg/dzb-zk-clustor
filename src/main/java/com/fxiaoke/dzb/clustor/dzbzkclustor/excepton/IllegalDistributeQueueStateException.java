package com.fxiaoke.dzb.clustor.dzbzkclustor.excepton;
/***
 *@author lenovo
 *@date 2019/5/19 8:53
 *@Description:
     IllegalDistributeQueueStateException
     分布式队列任务异常时
 *@version 1.0
 */
public class IllegalDistributeQueueStateException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private State state = State.other;
	
	public IllegalDistributeQueueStateException() {
		super();
	}
	public IllegalDistributeQueueStateException(String msg) {
		super(msg);
	}
	public IllegalDistributeQueueStateException(State illegalState) {
		super();
		state = illegalState;
	}
	public IllegalDistributeQueueStateException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public State getState() {return state;};
	
	public enum State{
		other,
		empty,
		full;
	}
}

