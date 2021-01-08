package com.mutao.pay.service;

import com.mutao.pay.demo.DemoBase;
import com.mutao.pay.sdk.AcpService;
import com.mutao.pay.sdk.LogUtil;
import com.mutao.pay.sdk.SDKConfig;
import com.mutao.pay.sdk.SDKUtil;
import com.mutao.pay.util.DateUtil;
import com.mutao.pay.util.OrderNoCenter;
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
 * @date 2020/12/2 15:17
 */
@Service
public class ApplyTokenService {
    @Value("${merId}")
    private String merId;

    public Map<String, String> applyToken() throws ServletException, IOException {
        String orderId = OrderNoCenter.getInstance().create();
        String txnTime = DateUtil.getTimeStrByDate(new Date());
        Map<String, String> contentData = new HashMap<String, String>();

        /***银联全渠道系统，产品参数，除了encoding自行选择外其他不需修改***/
        contentData.put("version", DemoBase.version);                  //版本号
        contentData.put("encoding", DemoBase.encoding);                //字符集编码 可以使用UTF-8,GBK两种方式
        contentData.put("signMethod", SDKConfig.getConfig().getSignMethod()); //签名方法
        contentData.put("txnType", "79");                              //交易类型 01-消费
        contentData.put("txnSubType", "05");                           //交易子类型 01-消费
        contentData.put("bizType", "000902");                          //业务类型 认证支付2.0
        contentData.put("channelType", "07");                          //渠道类型07-PC

        /***商户接入参数***/
        contentData.put("merId", merId);                   			   //商户号码（本商户号码仅做为测试调通交易使用，该商户号配置了需要对敏感信息加密）测试时请改成自己申请的商户号，【自己注册的测试777开头的商户号不支持代收产品】
        contentData.put("accessType", "0");                            //接入类型，商户接入固定填0，不需修改
        contentData.put("orderId", orderId);             			   //商户订单号，8-40位数字字母，不能含“-”或“_”，可以自行定制规则
        contentData.put("txnTime", txnTime);         				   //订单发送时间，格式为yyyyMMddHHmmss，必须取当前时间，否则会报txnTime无效
        contentData.put("tokenPayData", "{trId=99988877766&tokenType=01}"); //测试环境固定trId=99988877766&tokenType=01，生产环境由业务分配。测试环境因为所有商户都使用同一个trId，所以同一个卡获取的token号都相同，任一人发起更新token或者解除token请求都会导致原token号失效，所以之前成功、突然出现3900002报错时请先尝试重新开通一下。

        // 请求方保留域，
        // 透传字段，查询、通知、对账文件中均会原样出现，如有需要请启用并修改自己希望透传的数据。
        // 出现部分特殊字符时可能影响解析，请按下面建议的方式填写：
        // 1. 如果能确定内容不会出现&={}[]"'等符号时，可以直接填写数据，建议的方法如下。
//		contentData.put("reqReserved", "透传信息1|透传信息2|透传信息3");
        // 2. 内容可能出现&={}[]"'符号时：
        // 1) 如果需要对账文件里能显示，可将字符替换成全角＆＝｛｝【】“‘字符（自己写代码，此处不演示）；
        // 2) 如果对账文件没有显示要求，可做一下base64（如下）。
        //    注意控制数据长度，实际传输的数据长度不能超过1024位。
        //    查询、通知等接口解析时使用new String(Base64.decodeBase64(reqReserved), DemoBase.encoding);解base64后再对数据做后续解析。
//		contentData.put("reqReserved", Base64.encodeBase64String("任意格式的信息都可以".toString().getBytes(DemoBase.encoding)));

        /**对请求参数进行签名并发送http post请求，接收同步应答报文**/
        Map<String, String> reqData = AcpService.sign(contentData,DemoBase.encoding);			 //报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
        String requestBackUrl = SDKConfig.getConfig().getBackRequestUrl();   								 //交易请求url从配置文件读取对应属性文件acp_sdk.properties中的 acpsdk.backTransUrl
        Map<String, String> rspData = AcpService.post(reqData,requestBackUrl,DemoBase.encoding); //发送请求报文并接受同步应答（默认连接超时时间30秒，读取返回结果超时时间30秒）;这里调用signData之后，调用submitUrl之前不能对submitFromData中的键值对做任何修改，如果修改会导致验签不通过

        /**对应答码的处理，请根据您的业务逻辑来编写程序,以下应答码处理逻辑仅供参考------------->**/
        //应答码规范参考open.unionpay.com帮助中心 下载  产品接口规范  《平台接入接口规范-第5部分-附录》
        StringBuffer parseStr = new StringBuffer("");
        if(!rspData.isEmpty()){
            if(AcpService.validate(rspData, DemoBase.encoding)){
                LogUtil.writeLog("验证签名成功");
                String respCode = rspData.get("respCode") ;
                if(("00").equals(respCode)){
                    //成功
                    parseStr.append("<br>解析敏感信息加密信息如下（如果有）:<br>");
                    String customerInfo = rspData.get("customerInfo");
                    if(null!=customerInfo){
                        Map<String,String>  cm = AcpService.parseCustomerInfo(customerInfo, "UTF-8");
                        parseStr.append("customerInfo明文: " + cm+"<br>");
                    }
                    //如果是配置了敏感信息加密，如果需要获取卡号的明文，可以按以下方法解密卡号
                    String an = rspData.get("accNo");
                    if(null!=an){
                        an = AcpService.decryptData(an, "UTF-8");
                        parseStr.append("accNo明文: " + an);
                    }
                    String tokenPayData = rspData.get("tokenPayData");
                    if(null!=tokenPayData){
                        Map<String,String> tokenPayDataMap = SDKUtil.parseQString(tokenPayData.substring(1, tokenPayData.length() - 1));
                        String token = tokenPayDataMap.get("token");//这样取
                        parseStr.append("tokenPayDataMap明文: " + tokenPayDataMap +"<br>");
                    }
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
        String reqMessage = DemoBase.genHtmlResult(reqData);
        String rspMessage = DemoBase.genHtmlResult(rspData);
        Map<String, String> result = new HashMap<>();
        result.put("reqMessage", reqMessage);
        result.put("rspMessage", rspMessage);
        result.put("parseStr", parseStr.toString());
        return result;
    }
}
