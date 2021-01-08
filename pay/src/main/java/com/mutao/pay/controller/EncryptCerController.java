package com.mutao.pay.controller;

import com.mutao.pay.demo.DemoBase;
import com.mutao.pay.sdk.AcpService;
import com.mutao.pay.sdk.LogUtil;
import com.mutao.pay.sdk.SDKConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mutao
 * @version 1.0.0
 * @date 2020/12/2 15:26
 */
@RestController
@RequestMapping("encryptCer")
public class EncryptCerController {


    @GetMapping("/encryptCerUpdateQuery")
    public void encryptCerUpdateQuery(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
        SDKConfig.getConfig().loadPropertiesFromSrc();
        Map<String, String> contentData = new HashMap<String, String>();
        contentData.put("version", DemoBase.version);                  		     //版本号
        contentData.put("encoding", DemoBase.encoding);            		 //字符集编码 可以使用UTF-8,GBK两种方式
        contentData.put("signMethod", SDKConfig.getConfig().getSignMethod());    //签名方法  01:RSA证书方式  11：支持散列方式验证SHA-256 12：支持散列方式验证SM3
        contentData.put("txnType", "95");                              			 //交易类型 95-银联加密公钥更新查询
        contentData.put("txnSubType", "00");                           			 //交易子类型  默认00
        contentData.put("bizType", "000000");                          			 //业务类型  默认
        contentData.put("channelType", "07");                          			 //渠道类型

        contentData.put("certType", "01");							   			 //01：敏感信息加密公钥(只有01可用)
        contentData.put("merId", "777290058110048");                   			 //商户号码（商户号码777290058110097仅做为测试调通交易使用，该商户号配置了需要对敏感信息加密）测试时请改成自己申请的商户号，【自己注册的测试777开头的商户号不支持代收产品】
        contentData.put("accessType", "0");                            			 //接入类型，商户接入固定填0，不需修改
        contentData.put("orderId", DemoBase.getOrderId());             			 //商户订单号，8-40位数字字母，不能含“-”或“_”，可以自行定制规则
        contentData.put("txnTime", DemoBase.getCurrentTime());         		     //订单发送时间，格式为yyyyMMddHHmmss，必须取当前时间，否则会报txnTime无效                         //账号类型

        Map<String, String> reqData = AcpService.sign(contentData,DemoBase.encoding);			   //报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
        String requestBackUrl = SDKConfig.getConfig().getBackRequestUrl();				 			   //交易请求url从配置文件读取对应属性文件acp_sdk.properties中的 acpsdk.backTransUrl
        Map<String, String> rspData = AcpService.post(reqData,requestBackUrl,DemoBase.encoding);  //发送请求报文并接受同步应答（默认连接超时时间30秒，读取返回结果超时时间30秒）;这里调用signData之后，调用submitUrl之前不能对submitFromData中的键值对做任何修改，如果修改会导致验签不通过

        if(!rspData.isEmpty()){
            if(AcpService.validate(rspData, DemoBase.encoding)){
                LogUtil.writeLog("验证签名成功");
                String respCode = rspData.get("respCode") ;
                if(("00").equals(respCode)){
                    int resultCode = AcpService.updateEncryptCert(rspData,"UTF-8");
                    if (resultCode == 1) {
                        LogUtil.writeLog("加密公钥更新成功");
                    } else if (resultCode == 0) {
                        LogUtil.writeLog("加密公钥无更新");
                    } else {
                        LogUtil.writeLog("加密公钥更新失败");
                    }

                }else{
                    //其他应答码为失败请排查原因
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
    }
}
