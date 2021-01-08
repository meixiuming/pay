package com.mutao.pay.service;

import com.alibaba.fastjson.JSONObject;
import com.mutao.pay.demo.DemoBase;
import com.mutao.pay.sdk.AcpService;
import com.mutao.pay.sdk.CertUtil;
import com.mutao.pay.sdk.LogUtil;
import com.mutao.pay.sdk.SDKConfig;
import com.mutao.pay.util.DateUtil;
import com.mutao.pay.util.OrderNoCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mutao
 * @version 1.0.0
 * @date 2020/12/2 15:00
 */
@Service
public class ConsumeSMSService {
    Logger logger = LoggerFactory.getLogger(OpenSMSService.class);

    @Value("${merId}")
    private String merId;

    public Map<String, String> consumeSMS(String txnAmt,String phoneNo,String token,String orderId,String txnTime) throws ServletException, IOException {
//        String orderId = OrderNoCenter.getInstance().create();
//        String txnTime = DateUtil.getTimeStrByDate(new Date());
        Map<String, String> contentData = new HashMap<String, String>();

        /***银联全渠道系统，产品参数，除了encoding自行选择外其他不需修改***/
        contentData.put("version", DemoBase.version);                   //版本号
        contentData.put("encoding", DemoBase.encoding);            //字符集编码 可以使用UTF-8,GBK两种方式
        contentData.put("signMethod", SDKConfig.getConfig().getSignMethod()); //签名方法
        contentData.put("txnType", "77");                              //交易类型 11-代收
        contentData.put("txnSubType", "02");                           //交易子类型 02-消费短信
        contentData.put("bizType", "000902");                          //业务类型 认证支付2.0
        contentData.put("channelType", "07");                          //渠道类型07-PC

        /***商户接入参数***/
        contentData.put("merId", merId);                   			   //商户号码（本商户号码仅做为测试调通交易使用，该商户号配置了需要对敏感信息加密）测试时请改成自己申请的商户号，【自己注册的测试777开头的商户号不支持代收产品】
        contentData.put("accessType", "0");                            //接入类型，商户接入固定填0，不需修改
        contentData.put("orderId", orderId);             			   //商户订单号，8-40位数字字母，不能含“-”或“_”，可以自行定制规则
        contentData.put("txnTime", txnTime);         				   //订单发送时间，格式为yyyyMMddHHmmss，必须取当前时间，否则会报txnTime无效
        contentData.put("currencyCode", "156");						   //交易币种（境内商户一般是156 人民币）
        contentData.put("txnAmt", txnAmt);							   //交易金额，单位分，不要带小数点
        contentData.put("accType", "01");                              //账号类型

        //送手机号码
        Map<String,String> customerInfoMap = new HashMap<String,String>();
//        customerInfoMap.put("phoneNo", phoneNo);			        //手机短信验证码
//        String customerInfoStr = AcpService.getCustomerInfoWithEncrypt(customerInfoMap,null,DemoBase.encoding);

//        contentData.put("customerInfo", customerInfoStr);
        contentData.put("encryptCertId", CertUtil.getEncryptCertId());       //加密证书的certId，配置在acp_sdk.properties文件 acpsdk.encryptCert.path属性下
        //消费短信：token号（从前台开通的后台通知中获取或者后台开通的返回报文中获取）
        contentData.put("tokenPayData", "{token=" + token + "&trId=62000002975}");

//        String accNo = AcpService.encryptData("6217920112634501", "UTF-8");  //这里测试的时候使用的是测试卡号，正式环境请使用真实卡号
//
//        contentData.put("accNo", accNo);

        contentData.put("encryptCertId",AcpService.getEncryptCertId());
        /**对请求参数进行签名并发送http post请求，接收同步应答报文**/
        Map<String, String> reqData = AcpService.sign(contentData,DemoBase.encoding);			 //报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
        String requestBackUrl = SDKConfig.getConfig().getBackRequestUrl();   								 //交易请求url从配置文件读取对应属性文件acp_sdk.properties中的 acpsdk.backTransUrl
        Map<String, String> rspData = AcpService.post(reqData,requestBackUrl,DemoBase.encoding); //发送请求报文并接受同步应答（默认连接超时时间30秒，读取返回结果超时时间30秒）;这里调用signData之后，调用submitUrl之前不能对submitFromData中的键值对做任何修改，如果修改会导致验签不通过

        /**对应答码的处理，请根据您的业务逻辑来编写程序,以下应答码处理逻辑仅供参考------------->**/
        //应答码规范参考open.unionpay.com帮助中心 下载  产品接口规范  《平台接入接口规范-第5部分-附录》
        if(!rspData.isEmpty()){
            if(AcpService.validate(rspData, DemoBase.encoding)){
                LogUtil.writeLog("验证签名成功");
                String respCode = rspData.get("respCode") ;
                if(("00").equals(respCode)){
                    //成功
                    //TODO
                }else{
                    //其他应答码为失败请排查原因或做失败处理
                    //TODO
                }
            }else{
                LogUtil.writeErrorLog("验证签名失败");
                //TODO 检查验证签名失败的原因
            }
        }else{
            //未返回正确的http状态
            LogUtil.writeErrorLog("未获取到返回报文或返回http状态码非200");
        }
        logger.info("消费短信短信请求报文"+ JSONObject.toJSONString(reqData));
        logger.info("消费短信返回报文"+JSONObject.toJSONString(rspData));
        String reqMessage = DemoBase.genHtmlResult(reqData);
        String rspMessage = DemoBase.genHtmlResult(rspData);
        Map<String, String> result = new HashMap<>();
        result.put("reqMessage",reqMessage);
        result.put("rspMessage",rspMessage);
        return result;
    }

}
