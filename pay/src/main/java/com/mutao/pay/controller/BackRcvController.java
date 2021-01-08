package com.mutao.pay.controller;

import com.mutao.pay.demo.DemoBase;
import com.mutao.pay.sdk.AcpService;
import com.mutao.pay.sdk.LogUtil;
import com.mutao.pay.sdk.SDKConstants;
import com.mutao.pay.sdk.SDKUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;

/**
 * @author mutao
 * @version 1.0.0
 * @date 2020/12/2 15:20
 */
@RestController
@RequestMapping("backRcv")
public class BackRcvController {


    @PostMapping("/backRcvResponse")
    public void backRcvResponse(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LogUtil.writeLog("BackRcvResponse接收后台通知开始");
        String encoding = req.getParameter(SDKConstants.param_encoding);
        // 获取银联通知服务器发送的后台通知参数
        Map<String, String> reqParam = getAllRequestParam(req);
        LogUtil.printRequestLog(reqParam);
        //重要！验证签名前不要修改reqParam中的键值对的内容，否则会验签不过
        if (!AcpService.validate(reqParam, encoding)) {
            LogUtil.writeLog("验证签名结果[失败].");
            //验签失败，需解决验签问题
        } else {
            LogUtil.writeLog("验证签名结果[成功].");
            //交易成功，更新商户订单状态
            String orderId =reqParam.get("orderId"); //获取后台通知的数据，其他字段也可用类似方式获取
            String customerInfo = reqParam.get("customerInfo");
            if(null!=customerInfo){
                Map<String,String>  customerInfoMap = AcpService.parseCustomerInfo(customerInfo, "UTF-8");
                LogUtil.writeLog("customerInfoMap明文: "+ customerInfoMap);
            }
            String accNo = reqParam.get("accNo");
            //如果配置了敏感信息加密证书，可以用以下方法解密，如果不加密可以不需要解密
            if(null!=accNo){
                accNo = AcpService.decryptData(accNo, "UTF-8");
                LogUtil.writeLog("accNo明文: "+ accNo);
            }
            String tokenPayData = reqParam.get("tokenPayData");
            if(null!=tokenPayData){
                Map<String,String> tokenPayDataMap = SDKUtil.parseQString(tokenPayData.substring(1, tokenPayData.length() - 1));
                String token = tokenPayDataMap.get("token");//这样取
                LogUtil.writeLog("tokenPayDataMap明文: " + tokenPayDataMap);
            }

            String respCode = reqParam.get("respCode");
            //判断respCode=00、A6后，对涉及资金类的交易，请再发起查询接口查询，确定交易成功后更新数据库。
        }
        LogUtil.writeLog("BackRcvResponse接收后台通知结束");
        //返回给银联服务器http 200状态码
        resp.getWriter().print("ok");
    }


    /**
     * 获取请求参数中所有的信息
     * 当商户上送frontUrl或backUrl地址中带有参数信息的时候，
     * 这种方式会将url地址中的参数读到map中，会导多出来这些信息从而致验签失败，这个时候可以自行修改过滤掉url中的参数或者使用getAllRequestParamStream方法。
     * @param request
     * @return
     */
    public static Map<String, String> getAllRequestParam(
            final HttpServletRequest request) {
        Map<String, String> res = new HashMap<String, String>();
        Enumeration<?> temp = request.getParameterNames();
        if (null != temp) {
            while (temp.hasMoreElements()) {
                String en = (String) temp.nextElement();
                String value = request.getParameter(en);
                res.put(en, value);
                // 在报文上送时，如果字段的值为空，则不上送<下面的处理为在获取所有参数数据时，判断若值为空，则删除这个字段>
                if (res.get(en) == null || "".equals(res.get(en))) {
                    // System.out.println("======为空的字段名===="+en);
                    res.remove(en);
                }
            }
        }
        return res;
    }

    /**
     * 获取请求参数中所有的信息。
     * 非struts可以改用此方法获取，好处是可以过滤掉request.getParameter方法过滤不掉的url中的参数。
     * struts可能对某些content-type会提前读取参数导致从inputstream读不到信息，所以可能用不了这个方法。理论应该可以调整struts配置使不影响，但请自己去研究。
     * 调用本方法之前不能调用req.getParameter("key");这种方法，否则会导致request取不到输入流。
     * @param request
     * @return
     */
    public static Map<String, String> getAllRequestParamStream(
            final HttpServletRequest request) {
        Map<String, String> res = new HashMap<String, String>();
        try {
            String notifyStr = new String(IOUtils.toByteArray(request.getInputStream()), DemoBase.encoding);
            LogUtil.writeLog("收到通知报文：" + notifyStr);
            String[] kvs= notifyStr.split("&");
            for(String kv : kvs){
                String[] tmp = kv.split("=");
                if(tmp.length >= 2){
                    String key = tmp[0];
                    String value = URLDecoder.decode(tmp[1],DemoBase.encoding);
                    res.put(key, value);
                }
            }
        } catch (UnsupportedEncodingException e) {
            LogUtil.writeLog("getAllRequestParamStream.UnsupportedEncodingException error: " + e.getClass() + ":" + e.getMessage());
        } catch (IOException e) {
            LogUtil.writeLog("getAllRequestParamStream.IOException error: " + e.getClass() + ":" + e.getMessage());
        }
        return res;
    }
}

