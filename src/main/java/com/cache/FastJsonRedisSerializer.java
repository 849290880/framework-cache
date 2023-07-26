package com.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class FastJsonRedisSerializer<T> implements RedisSerializer<T> {
    private Class<T> clazz;

    static {
        // 添加 autoType 的支持
        // 注意：这里是全局设置，可能会带来安全风险，请根据实际情况决定是否开启
         ParserConfig.getGlobalInstance().addAccept("com.rsh.");
        ParserConfig.getGlobalInstance().addAccept("com.cache.");
    }

    public FastJsonRedisSerializer(Class<T> clazz) {
        super();
        this.clazz = clazz;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        return JSON.toJSONBytes(t, SerializerFeature.WriteClassName);
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        return JSON.parseObject(bytes, clazz);
    }
}