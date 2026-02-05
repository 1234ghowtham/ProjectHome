package redisconnection;

import redis.clients.jedis.Jedis;

//Bill Pugh Singleton
public class RedisService {

	private Jedis jedis;

	private RedisService() {
		try {
			jedis = new Jedis("localhost", 6379);
			jedis.connect();
			System.out.println("Redis connected successfully.");
		} catch (Exception e) {
			System.out.println("Failed to connect to Redis: " + e.getMessage());
		}
	}

	private static class Holder {
		private static final RedisService INSTANCE = new RedisService();
	}

	public static RedisService getInstance() {
		return Holder.INSTANCE;
	}

	public Jedis getClient() {
		return jedis;
	}

	public void close() {
		if (jedis != null) {
			jedis.close();
			System.out.println("Redis connection closed.");
		}
	}
}
