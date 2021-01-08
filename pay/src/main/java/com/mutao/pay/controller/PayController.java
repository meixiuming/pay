package com.mutao.pay.controller;


import com.alibaba.fastjson.JSONObject;
import com.mutao.pay.service.*;


import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;


/**
 * 验证码操作处理
 *
 * @author mutao
 * @version 1.0.0
 * @date 2020/11/13 13:43
 */
@RestController
@RequestMapping("pay")
public class PayController {

    Logger logger = LoggerFactory.getLogger(PayController.class);

    @Resource
    private OpenCardService openCardService;

    @Resource
    private OpenQueryService openQueryService;

    @Resource
    private DeleteTokenService deleteTokenService;

    @Resource
    private UpdateTokenService updateTokenService;

    @Resource
    private ConsumeSMSService consumeSMSService;

    @Resource
    private OpenSMSService openSMSService;

    @Resource
    private ConsumeService consumeService;
    @Resource
    private ApplyTokenService applyTokenService;
    @Resource
    private CancelService cancelService;

    @Resource
    private RefundService refundService;
    @Resource
    private QueryService queryService;
    @Resource
    private FileTransferService fileTransferService;
    @Resource
    private OpenCardFrontService openCardFrontService;

    /**
     * 银联侧开通：前台交易，有前台通知，后通知
     * 交易说明:后台通知或者发起开通查询交易（根据开通交易的orderId）获取卡的token号。
     */
    @PostMapping("/openCardFront")
    public void openCardFront (HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
        StringBuffer sb=new StringBuffer();
        String s=null;
        while((s=br.readLine())!=null){
            sb.append(s);
        }
        JSONObject jsonObject = JSONObject.parseObject(sb.toString());
        String accNo = req.getParameter("accNo");
        String phoneNo = req.getParameter("phoneNo");
        Map <String,String>result =openCardFrontService.openCardFront(accNo,phoneNo);
        resp.getWriter().write("请求报文:<br/>"+result.get("reqMessage")+"<br/>" + "应答报文:</br>"+result.get("rspMessage")+result.get("parseStr"));
    }

    /**
     * 后台支付开通
     * 交易说明:后台开通只支持信用卡,后台同步应答确定交易成功。
     */
    @PostMapping("/openCard")
    public void openCard(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
        String accNo = req.getParameter("accNo");
        Map <String,String>result =openCardService.openCard(accNo);
        resp.getWriter().write("请求报文:<br/>"+result.get("reqMessage")+"<br/>" + "应答报文:</br>"+result.get("rspMessage")+result.get("parseStr"));
    }

    /**
     * 查询开通：后台交易，无通知
     * 交易说明:1）使用此交易获取token号。
     *   2) 只能针对前台开通交易（银联侧）做查询。
     */
    @PostMapping("/openQuery")
    public void openQuery(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
        String orderId = req.getParameter("orderId");
        Map <String,String>result =openQueryService.openCard(orderId);
        resp.getWriter().write("请求报文:<br/>"+result.get("reqMessage")+"<br/>" + "应答报文:</br>"+result.get("rspMessage")+result.get("parseStr"));
    }

    /**
     * 删除token号：后台交易，无通知
     * 删除token后重新申请token号会发生变化。
     */

    @PostMapping("/deleteToken")
    public void deleteToken(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
        String token = req.getParameter("token");
        Map <String,String>result =deleteTokenService.deleteToken(token);
        resp.getWriter().write("请求报文:<br/>"+result.get("reqMessage")+"<br/>" + "应答报文:</br>"+result.get("rspMessage")+result.get("parseStr"));
    }
    /**
     * 更新token号：后台交易，无通知<br>
     * 交易说明: 同步应答确定交易成功
     */
    @PostMapping("/updateToken")
    public void updateToken(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
        String token = req.getParameter("token");
        Map <String,String>result =updateTokenService.updateToken(token);
        resp.getWriter().write("请求报文:<br/>"+result.get("reqMessage")+"<br/>" + "应答报文:</br>"+result.get("rspMessage")+result.get("parseStr"));
    }

    /**
     * 开通短信：后台交易，
     * 交易说明: 同步应答确定交易成功。
     * 金额  + 卡号  + 手机号phoneNo（customerInfo域），同步应答确定交易成功。
     */
    @PostMapping("/openSMS")
    @ResponseBody
    public void openSMS(@RequestBody JSONObject jsonObject,HttpServletResponse resp)throws ServletException, IOException {
//        String accNo = req.getParameter("accNo");
        String accNo = jsonObject.getString("accNo");
        String phoneNo = jsonObject.getString("phoneNo");
        String token = jsonObject.getString("token");
        Map <String,String>result =openSMSService.openSMS(accNo,phoneNo, token);
        logger.info(JSONObject.toJSONString(result));
        resp.getWriter().write("请求报文:<br/>"+result.get("reqMessage")+"<br/>" + "应答报文:</br>"+result.get("rspMessage")+result.get("parseStr"));
    }

    /**
     * 消费短信：后台交易，
     * 交易说明: 同步应答确定交易成功。
     * 金额  + token号  + 手机号phoneNo（customerInfo域），同步应答确定交易成功。
     */
    @PostMapping("/consumeSMS")
    @ResponseBody

    public void consumeSMS(@RequestBody JSONObject jsonObject, HttpServletResponse resp)throws ServletException, IOException {
        String txnAmt =  jsonObject.getString("txnAmt");
        String token =  jsonObject.getString("token");
        String phoneNo =  jsonObject.getString("phoneNo");
        String orderId =  jsonObject.getString("orderId");
        String txnTime =  jsonObject.getString("txnTime");

        Map<String, String> result = consumeSMSService.consumeSMS(txnAmt, phoneNo, token,orderId,txnTime);
        logger.info(JSONObject.toJSONString(result));

        resp.getWriter().write("请求报文:<br/>" + result.get("reqMessage") + "<br/>" + "应答报文:</br>" + result.get("rspMessage") + result.get("parseStr"));
    }

    /**
     * 消费：后台资金类交易
     *易说明:1）确定交易成功机制：商户需开发后台通知接口或交易状态查询接口（Query）确定交易是否成功，建议发起查询交易的机制：可查询N次（不超过6次），每次时间间隔2N秒发起,即间隔1，2，4，8，16，32S查询（查询到03，04，05继续查询，否则终止查询）
     *      2）交易要素token号+短息验证码(默认验证短信，如果配置了不验证短信则不送短信验证码）
     */
    @PostMapping("/consume")
    @ResponseBody
    public void consume(@RequestBody JSONObject jsonObject, HttpServletResponse resp)throws ServletException, IOException {
        String txnAmt =  jsonObject.getString("txnAmt");
        String token =  jsonObject.getString("token");
        String smsCode =  jsonObject.getString("smsCode");
        String orderId =  jsonObject.getString("orderId");
        String txnTime =  jsonObject.getString("txnTime");
        Map <String,String>result =consumeService.consume( txnAmt, token, smsCode, orderId, txnTime);
        logger.info(JSONObject.toJSONString(result));

        resp.getWriter().write("请求报文:<br/>"+result.get("reqMessage")+"<br/>" + "应答报文:</br>"+result.get("rspMessage")+result.get("parseStr"));
    }

    /**
     * 申请token号
     * 交易说明:根据开通并付款交易的orderId申请,后台同步应答确定交易成功。
     */
    @PostMapping("/applyToken")
    public void applyToken(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
        String txnAmt = req.getParameter("txnAmt");
        Map <String,String>result =applyTokenService.applyToken();
        resp.getWriter().write("请求报文:<br/>"+result.get("reqMessage")+"<br/>" + "应答报文:</br>"+result.get("rspMessage")+result.get("parseStr"));
    }

    /**
     * 交易：消费撤销：后台资金类交易，有同步应答和后台通知应答<br>
     * 交易说明:1）确定交易成功机制：商户必须开发后台通知接口和交易状态查询接口（Form09_6_5_Query）确定交易是否成功，建议发起查询交易的机制：可查询N次（不超过6次），每次时间间隔2N秒发起,即间隔1，2，4，8，16，32S查询（查询到03，04，05继续查询，否则终止查询）
     *        2）消费撤销仅能对当清算日的消费做，必须为全额，一般当日或第二日到账。
     */
    @PostMapping("/cancel")
    @ResponseBody
    public void cancel(@RequestBody JSONObject jsonObject, HttpServletResponse resp)throws ServletException, IOException {
        String txnAmt =  jsonObject.getString("txnAmt");
        String origQryId =  jsonObject.getString("origQryId");
        Map <String,String>result =cancelService.cancel( txnAmt,  origQryId);
        resp.getWriter().write("请求报文:<br/>"+result.get("reqMessage")+"<br/>" + "应答报文:</br>"+result.get("rspMessage")+result.get("parseStr"));
    }
    /**
     * 交易：消费撤销：后台资金类交易，有同步应答和后台通知应答<br>
     * 交易说明： 1）确定交易成功机制：商户必须开发后台通知接口和交易状态查询接口（Form09_6_5_Query）确定交易是否成功，建议发起查询交易的机制：可查询N次（不超过6次），每次时间间隔2N秒发起,即间隔1，2，4，8，16，32S查询（查询到03，04，05继续查询，否则终止查询）
     *         2）退货金额不超过总金额，可以进行多次退货
     *        3）退货能对11个月内的消费做（包括当清算日），支持部分退货或全额退货，到账时间较长，一般1-10个清算日（多数发卡行5天内，但工行可能会10天），所有银行都支持
     **/
    @PostMapping("/refund")
    public void refund(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
        String txnAmt = req.getParameter("txnAmt");
        String origQryId = req.getParameter("origQryId");
        Map <String,String>result =refundService.refund( txnAmt,  origQryId);
        resp.getWriter().write("请求报文:<br/>"+result.get("reqMessage")+"<br/>" + "应答报文:</br>"+result.get("rspMessage")+result.get("parseStr"));
    }

    /**
     * 交易：交易状态查询交易：只有同步应答
     * 交易说明：消费同步返回00，如果未收到后台通知建议发起查询交易，可查询N次（不超过6次），每次时间间隔2N秒发起,即间隔1，2，4，8，16，32S查询（查询到03 04 05继续查询，否则终止查询）。【如果最终尚未确定交易是否成功请以对账文件为准】
     *         消费同步返03 04 05响应码及未得到银联响应（读超时）建议发起查询交易，可查询N次（不超过6次），每次时间间隔2N秒发起,即间隔1，2，4，8，16，32S查询（查询到03 04 05继续查询，否则终止查询）。【如果最终尚未确定交易是否成功请以对账文件为准】
     *
     */
    @PostMapping("/query")
    @ResponseBody
    public void query(@RequestBody JSONObject jsonObject, HttpServletResponse resp)throws ServletException, IOException {
        String txnTime =  jsonObject.getString("txnTime");
        String orderId =  jsonObject.getString("orderId");
        Map <String,String>result =queryService.query(orderId,txnTime);
        resp.getWriter().write("请求报文:<br/>"+result.get("reqMessage")+"<br/>" + "应答报文:</br>"+result.get("rspMessage")+result.get("parseStr"));
    }

    /**
     * 交易：消费撤销：后台资金类交易，有同步应答和后台通知应答<br>
     * 交易说明:1）确定交易成功机制：商户必须开发后台通知接口和交易状态查询接口（Form09_6_5_Query）确定交易是否成功，建议发起查询交易的机制：可查询N次（不超过6次），每次时间间隔2N秒发起,即间隔1，2，4，8，16，32S查询（查询到03，04，05继续查询，否则终止查询）
     *        2）消费撤销仅能对当清算日的消费做，必须为全额，一般当日或第二日到账。
     */
    @PostMapping("/fileTransfer")
    public void fileTransfer(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
        String settleDate = req.getParameter("settleDate");
        Map <String,String>result =fileTransferService.fileTransfer(settleDate);
        resp.getWriter().write("请求报文:<br/>"+result.get("reqMessage")+"<br/>" + "应答报文:</br>"+result.get("rspMessage")+result.get("parseStr"));
    }
}
