package com.mutao.pay.util;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.springframework.web.multipart.MultipartFile;

/**
 * 网络请求工具类
 *
 * @author wansishuang
 * @date 2019-08-03
 */
public class OkHttpUtil {
	private final static Logger log = LoggerFactory.getLogger(OkHttpUtil.class);
	private static OkHttpClient okHttpClient = new OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS)
			.writeTimeout(2, TimeUnit.SECONDS).readTimeout(4, TimeUnit.SECONDS).build();

	private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");


	/**
	 * JSON方式POST请求【直接传入json字符串】
	 *
	 * @param url
	 * @param param
	 * @return
	 */
	public static JSONObject syncPostJSONString(String url, String param) {
		//MediaType  设置Content-Type 标头中包含的媒体类型值
		RequestBody body = FormBody.create(JSON_TYPE, param);
		String result = postWithHeader(url, null, body, null);
		if (result != null) {
			return JSON.parseObject(result);
		} else {
			return null;
		}
	}
	/**
	 * 同步请求JSON
	 * 
	 * @param url   请求地址
	 * @param param json对象
	 * @return null为请求失败
	 */
	public static JSONObject syncPostJSON(String url, JSONObject param) {
		RequestBody body = RequestBody.create(JSON_TYPE, param.toJSONString());
		String result = postWithHeader(url, null, body, null);
		log.info("***************888********"+result);
		if (result != null) {
			return JSON.parseObject(result);
		} else {
			return null;
		}
	}
	
	/**
	 * 异步请求Map
	 * 
	 * @param url   请求地址
	 * @return null为请求失败  // TODO
	 */
	public static void asyncPost(String url, Map<String, String> reqMap, Callback callback) {
		RequestBody body = RequestBody.create(JSON_TYPE, JSONObject.toJSONString(reqMap));
		postWithHeader(url, null, body, callback);
	}

	/**
	 * 带header同步请求JSON
	 * 
	 * @param url     请求地址
	 * @param headers 请求头描述
	 * @param param   请求参数
	 * @return null为请求失败
	 */
	public static JSONObject syncPostJSON(String url, Map<String, String> headers, JSONObject param) {
		RequestBody body = RequestBody.create(JSON_TYPE, param.toJSONString());
		String result = postWithHeader(url, headers, body, null);
		if (result != null) {
			return JSON.parseObject(result);
		} else {
			return null;
		}
	}

	/**
	 * 同步请求JSON 请求内容为表单类型
	 * 
	 * @param url
	 * @param param
	 * @return null为请求失败
	 */
	public static JSONObject syncPostFormData(String url, Map<String, Object> param) {
		FormBody.Builder builder = new FormBody.Builder();
		if (param != null) {
			for (Map.Entry<String, Object> entry : param.entrySet()) {
				builder.add(entry.getKey(), entry.getValue().toString());
			}
		}
		String result = postWithHeader(url, null, builder.build(), null);
		log.info("表单请求返回结果"+result);
		if (result != null) {
			return JSON.parseObject(result);
		} else {
			return null;
		}
	}

	/**
	 * 同步请求
	 * 
	 * @param url
	 * @return null为请求失败
	 */
	public static String syncGet(String url) {
		return getWithHeader(url, null, null);
	}

	/**
	 * 异步请求
	 * 
	 * @param url      请求地址
	 * @param callBack 异步结果
	 * @return null为请求失败
	 */
	public static String asyncGet(String url, Callback callBack) {
		return getWithHeader(url, null, callBack);
	}

	private static String getWithHeader(String url, Map<String, String> headers, Callback callBack) {
		try {
			Request req = null;
			if (headers != null) {
				okhttp3.Headers.Builder builder = new okhttp3.Headers.Builder();
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					builder.add(entry.getKey(), entry.getValue());
				}
				Headers header = builder.build();
				req = new Request.Builder().headers(header).url(url).build();
			} else {
				req = new Request.Builder().url(url).build();
			}
			if (callBack == null) {
				Response res = okHttpClient.newCall(req).execute();
				return res.body().string();
			} else {
				okHttpClient.newCall(req).enqueue(callBack);
				return "请求成功";
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error("get请求" + url + "失败");
			return null;
		}
	}

	private static String postWithHeader(String url, Map<String, String> headers, RequestBody body, Callback callBack) {
		try {
			Request req = null;
			if (headers != null) {
				okhttp3.Headers.Builder builder = new okhttp3.Headers.Builder();
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					builder.add(entry.getKey(), entry.getValue());
				}
				Headers header = builder.build();
				req = new Request.Builder().headers(header).url(url).post(body).build();
			} else {
				req = new Request.Builder().url(url).post(body).build();
			}
			if (callBack == null) {
				Response res = okHttpClient.newCall(req).execute();
				return res.body().string();
			} else {
				okHttpClient.newCall(req).enqueue(callBack);
				return "请求成功";
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error("post请求" + url + "失败,请求为" + JSONObject.toJSONString(body));
			return null;
		}
	}

	/**
	 * 同时提交参数和多媒体文件
	 * @param url
	 * @param params
	 * @param fileMap
	 * @return
	 */
	public static String postMulFile(String url, JSONObject params, Map<String, MultipartFile> fileMap) {
		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
//		RequestBody json = FormBody.create(params,MediaType.parse("application/json;charset=utf-8"));
		RequestBody json = FormBody.create(JSONObject.toJSONString(params), MediaType.parse("application/json;charset=UTF-8"));
		builder.addPart(Headers.of("Content-Disposition", "form-data;name=params"), json);
		try {
			for (String key : fileMap.keySet()) {
				RequestBody fileBody = RequestBody.create(fileMap.get(key).getBytes(),MediaType.parse("application/octet-stream"));
				builder.addPart(Headers.of("Content-Disposition", "form-data;name="+key+";filename=anyname"), fileBody);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		RequestBody body= builder.build();
		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();
		String result = "";
		try {
			Response response = okHttpClient.newCall(request).execute();
			if (response.isSuccessful()){
				result= JSONObject.toJSONString(response.body());
			}else{
				result= "请求失败，错误码："+response.code()+"\t失败信息："+ response.message();
			}
		} catch (IOException e) {
			e.printStackTrace();
			result= "调用服务异常："+e.getMessage();

		}
		return result;
	}

}
