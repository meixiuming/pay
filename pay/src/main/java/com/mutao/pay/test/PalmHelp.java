package com.mutao.pay.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.Security;

public class PalmHelp {


	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	private static final String SM4_CBC="SM4/CBC/PKCS5Padding";

	/**
	 * base64 掌静脉特征加密
	 * @param value
	 * @param symkey 堆成密钥 平台分发    9480A76484CB841E94018FA3F3BE421F
	 * @param secrect 签名密钥 平台分发  Z2sLrZy0X4TJ4kXN
	 * @return
	 * @throws Exception
	 */
	public static String encrypt(String value,String symkey,String secrect) throws Exception {
		Cipher cipher = Cipher.getInstance(SM4_CBC, BouncyCastleProvider.PROVIDER_NAME);
		Key sm4key = new SecretKeySpec(CommonHelp.hexToBytes(symkey), "SM4");
		IvParameterSpec iv = new IvParameterSpec(secrect.getBytes());
		cipher.init(Cipher.ENCRYPT_MODE, sm4key, iv);
		byte[] result = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
		return Base64.toBase64String(result);
	}


	private static byte[] hash(byte[] srcData) {
		SM3Digest digest = new SM3Digest();
		digest.update(srcData, 0, srcData.length);
		byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);
		return hash;
	}


	/**
	 * @param json 请求参数
	 * @param appId 接入方ID 平台分发
	 * @param secrect 签名密钥 平台分发
	 * @return
	 */
	public static JSONObject signSM3PostBody(JSONObject json,String appId,String secrect) {
		json.put("appId", appId);
		json.put("secrect", secrect);
		json.put("signMethod", "SM3");
		String str = JSON.toJSONString(json, SerializerFeature.MapSortField);
		byte[] srcData = str.getBytes(StandardCharsets.UTF_8);
		String signature=ByteUtils.toHexString(hash(srcData));
		json.put("signature", signature);
		json.remove("secrect");
		return json;
	}
}

