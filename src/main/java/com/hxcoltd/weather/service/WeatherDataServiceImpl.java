package com.hxcoltd.weather.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.hxcoltd.weather.vo.WeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * WeatherDataService 实现.
 * 
 * @since 1.0.0 2017年11月22日
 * @author <a href="https://waylau.com">Way Lau</a> 
 */
@Service
public class WeatherDataServiceImpl implements WeatherDataService {
    private static final Logger logger= LoggerFactory.getLogger(WeatherDataService.class);
	private static final String WEATHER_URI = "http://wthrcdn.etouch.cn/weather_mini?";
	private static final Long TIME_OUT = 30L;
	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	
	@Override
	public WeatherResponse getDataByCityId(String cityId) {
		String uri = WEATHER_URI + "citykey=" + cityId;
		return this.doGetWeahter(uri);
	}

	@Override
	public WeatherResponse getDataByCityName(String cityName) {
		String uri = WEATHER_URI + "city=" + cityName;
		return this.doGetWeahter(uri);
	}

	/**
	 * 使用Redis，如果缓存有，使用缓存，没有远程访问，并加入到redis中
	 * @param uri
	 * @return
	 */
	private WeatherResponse doGetWeahter(String uri) {
		String key = uri;
		String strBody = null;
        WeatherResponse resp = null;
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

        if (stringRedisTemplate.hasKey(key)){
            logger.info("Redis has data");
            strBody = ops.get(key);

        }else{
            logger.info("Redis don't has data");
            // 缓存没有，再调用服务接口来获取

            //sprign rest 的一个客户端 restTemplate 用于将json序列化为一个ResponseEntity 的string对象
            ResponseEntity<String> respString = restTemplate.getForEntity(uri, String.class);


            if (respString.getStatusCodeValue() == 200) {
                strBody = respString.getBody();
            }

            // 数据写入缓存
            ops.set(key,strBody,TIME_OUT, TimeUnit.SECONDS);
        }

		try {

            //利用jackson来转换为对象
            ObjectMapper mapper = new ObjectMapper();

            //利用jackson来转换为对象
			resp = mapper.readValue(strBody, WeatherResponse.class);
		} catch (IOException e) {
		    logger.error("error！",e);
		}
		
		return resp;
	}

	@Override
	public void syncDateByCityId(String cityId) {
		String uri = WEATHER_URI + "citykey=" + cityId;
		this.saveWeatherData(uri);
	}

	/**
	 * 把天气数据放在缓存
	 * @param uri
	 */
	private void saveWeatherData(String uri) {
		String key = uri;
		String strBody = null;
		ValueOperations<String, String>  ops = stringRedisTemplate.opsForValue();

		// 调用服务接口来获取
		ResponseEntity<String> respString = restTemplate.getForEntity(uri, String.class);

		if (respString.getStatusCodeValue() == 200) {
			strBody = respString.getBody();
		}

		// 数据写入缓存
		ops.set(key, strBody, TIME_OUT, TimeUnit.SECONDS);

	}

}
