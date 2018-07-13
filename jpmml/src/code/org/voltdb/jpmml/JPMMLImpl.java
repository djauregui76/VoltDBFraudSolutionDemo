package org.voltdb.jpmml;

public class JPMMLImpl {

	private static final long EVICT_CYCLE = 60 * 1000 * 1440;
	private static final int MIN_IDLE = 8;
	private static final int MAX_TOTAL = 128;

	JPMMLImplPool thePool = null;

	private static JPMMLImpl instance = null;
	
	protected JPMMLImpl() {
	}

	public static JPMMLImpl getInstance() {
		if (instance == null) {
			synchronized (JPMMLImpl.class) {
				instance = new JPMMLImpl();
			}
		}
		return instance;
	}

	public JPMMLImplPool getPool() {
		
		if (thePool == null) {
			thePool = new JPMMLImplPool(new JPMMLImplPoolFactory());
			thePool.setMinEvictableIdleTimeMillis(1000);
			thePool.setTestWhileIdle(true);
			thePool.setTimeBetweenEvictionRunsMillis(EVICT_CYCLE);
			thePool.setMinIdle(MIN_IDLE);
			thePool.setMaxTotal(MAX_TOTAL);
			thePool.setBlockWhenExhausted(true);
		}
		
		return thePool;
	}

	public void clearPool() {
		if (thePool != null) {
			thePool.clear();
		}
	}
	

}
