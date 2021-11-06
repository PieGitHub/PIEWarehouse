package com.erp.storage.controller;

import com.erp.aoplog.MyLog;
import com.erp.bean.*;
import com.erp.market.biz.CustomBiz;
import com.erp.market.biz.OrdersBiz;
import com.erp.market.dao.OrdersDao;
import com.erp.storage.biz.CkWarehouseBiz;
import com.erp.storage.biz.KcWarehouseBiz;
import com.erp.storage.biz.WarehouseBiz;
import com.erp.storage.dao.WarehouseDao;
import com.github.pagehelper.PageInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import javax.websocket.server.PathParam;
import java.util.List;
@EnableTransactionManagement
@Controller
@RequestMapping("/storage/delivery")
public class CkWarehouseController {
    @Autowired
    private CkWarehouseBiz ckbiz;
    @Autowired
    private OrdersDao odao;
    @Autowired
    private OrdersBiz obiz;
    @Autowired
    WarehouseDao wdao;
    @Autowired
    KcWarehouseBiz kcbiz;
    @Autowired
    WarehouseBiz wbiz;

    //每次执行前会优先 将mid放入request范围
    @ModelAttribute
    public void initModuleData(HttpSession session) {
        session.removeAttribute("mid");
        session.setAttribute("mid", 12);
    }

    @RequestMapping("/cklist")
    @MyLog(value = "查看出库列表")
    public String into(HttpSession session,CkWarehouse ck, Model model, @RequestParam(value ="pageNo",defaultValue = "1") Integer pageNo, @RequestParam(value ="pageSize",defaultValue = "5")Integer pageSize){
        ck.setIndent(null);
        ck.setState(null);
        Users loguser = (Users)session.getAttribute("loguser");
        PageInfo<CkWarehouse> ckByPage =null;
        if (loguser.getJobId()==1 ||loguser.getJobId()==6){
            ckByPage=ckbiz.findAllPage(null, pageNo, pageSize);
        }else {
            ckByPage=ckbiz.findAllPage(loguser.getuId(), pageNo, pageSize);
        }

        List<CkWarehouse> cklist = ckByPage.getList();
        model.addAttribute("ckPage",ckByPage);
        model.addAttribute("cklist",cklist);
        model.addAttribute("ck",ck);
        return "storage/delivery/deliveryList";
    }
    @RequestMapping("/check")
    @MyLog(value = "查看出库列表")
    public String findckpage(HttpSession session,Model model,CkWarehouse ck, @RequestParam(value ="pageNo",defaultValue = "1") Integer pageNo, @RequestParam(value ="pageSize",defaultValue = "5")Integer pageSize){
        Users loguser = (Users)session.getAttribute("loguser");
        PageInfo<CkWarehouse> ckByPage =null;
        if (loguser.getJobId()==1 ||loguser.getJobId()==6){
            ckByPage = ckbiz.findAllByPage(null,ck.getWarehouse().getcName(), ck.getIndent(), ck.getState(), pageNo, pageSize);
        }else {
            ckByPage = ckbiz.findAllByPage(null,ck.getWarehouse().getcName(), ck.getIndent(), ck.getState(), pageNo, pageSize);
        }
        List<CkWarehouse> cklist = ckByPage.getList();
        model.addAttribute("ckPage",ckByPage);
        model.addAttribute("cklist",cklist);
        model.addAttribute("ck",ck);
        return "storage/delivery/deliveryList";
    }

    /**
     * 出库详情
     * @param model
     * @param indent 出库indent
     * @return
     */
    @RequestMapping("/view/{indent}")
    @MyLog(value = "查看出库详情")
    public String deliveryView(Model model,@PathVariable("indent") String indent){
        CkWarehouse ckWarehouse = ckbiz.findCkById(indent);
        List<Orderdetails> orderdetails = obiz.selectOrderdetailsByorderid(ckWarehouse.getOrders().getOrderid());
        model.addAttribute("orderlist",orderdetails);
        model.addAttribute("ck",ckWarehouse);
        return "storage/delivery/deliveryView";
    }

    /**
     * 跳转添加出库页面
     * @param model
     * @return
     */
    @RequestMapping("/goadd")
    public String goadd(Model model){
        List<Warehouse> warehouselist = wdao.findAllBystate1();
        List<Orders> orderslist = odao.selectAllck();

        model.addAttribute("warehouselist",warehouselist);
        model.addAttribute("orderslist",orderslist);
        return "storage/delivery/deliveryAdd";
    }

    /**
     * 添加仓库
     * @param ckWarehouse
     * @return
     */
    @RequestMapping("/add")
    @MyLog(value = "添加出库信息")
    public String add(CkWarehouse ckWarehouse){
        Boolean flas = ckbiz.insertSelective(ckWarehouse);
        return "forward:/storage/delivery/cklist";
    }

    /**
     * 发货 取消订单 确定汇款等等
     * @param id 出库id
     * @param state 要改变的状态
     * @param pageNo1
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @RequestMapping("/cstate")
    @MyLog(value = "发货、取消订单等")
    public String cstate(@RequestParam("id") Integer id,@RequestParam("state1") String state1,@RequestParam("state") String state,@RequestParam("pageNo1") Integer pageNo1){

            CkWarehouse ckWarehouse = ckbiz.selectByPrimaryKey(id);//根据出库id查询入库信息
            System.out.println("仓库id："+ckWarehouse.getWarehouseId());//仓库id
            System.out.println("入库订单id："+ckWarehouse.getIndent());//入库订单
            CkWarehouse ck = ckbiz.findCkById(ckWarehouse.getIndent());//仓库详情
            List<Orderdetails> orderdetails = obiz.selectOrderdetailsByorderid(ckWarehouse.getIndent());
            Integer cName= ck.getWarehouse().getId();//仓库名
            Integer brandName=null;//品牌id
            Integer typeName=null;//类型id
            Integer pModel=null;//型号id
            Integer firmName=null;//厂商id
            Integer count=null;//数量
            Double wmoney=null;//金额
            Double money=ck.getWarehouse().getwMoney();//金额
            try {
                //发货  库存减少
                if (state.equals("2")){
                    for (Orderdetails od:orderdetails) {
                        brandName = od.getBrand().getBrandId();
                        typeName=od.getType().getTypeId();
                        pModel=od.getProduct().getProductId();
                        count=od.getPurchaseNum();
                        wmoney=od.getPrototal().doubleValue();
                        List<KcWarehouse> kc = kcbiz.findKcByRk(cName, brandName, typeName, pModel, firmName);
                        if (kc.size()==0 || kc.get(0).getRepertory()<count){
                            throw new RuntimeException();
                        }else {
                            KcWarehouse kcWarehouse = kc.get(0);//得到第一个库存
                            System.out.println("库存id    "+kcWarehouse.getId());
                            count=-count;
                            kcbiz.updateKcRepertoryById(kcWarehouse.getId(),count);
                        }
                    }
                }
                //取消订单  库存加
                if (state.equals("4")){
                    for (Orderdetails od:orderdetails) {
                        brandName = od.getBrand().getBrandId();
                        typeName=od.getType().getTypeId();
                        pModel=od.getProduct().getProductId();
                        count=od.getPurchaseNum();
                        wmoney=od.getPrototal().doubleValue();
                        List<KcWarehouse> kc = kcbiz.findKcByRk(cName, brandName, typeName, pModel, firmName);
                        if (kc.size()==0){
                            throw new RuntimeException();
                        }else {
                            if (state1.equals("2")){
                                //发货状态下点取消订单，先让仓库金额加
                                wbiz.updatemoneyById(ckWarehouse.getWarehouseId(),wmoney,money);
                            }
                            KcWarehouse kcWarehouse = kc.get(0);//得到第一个库存
                            System.out.println("库存id    "+kcWarehouse.getId());
                            kcbiz.updateKcRepertoryById(kcWarehouse.getId(),count);
                        }
                    }
                }
                //确认已退货  仓库金额减少
                if (state.equals("5")){
                    if (!state1.equals("1")){
                        for (Orderdetails od:orderdetails) {
                            brandName = od.getBrand().getBrandId();
                            typeName=od.getType().getTypeId();
                            pModel=od.getProduct().getProductId();
                            count=od.getPurchaseNum();
                            wmoney=od.getPrototal().doubleValue();
                            List<KcWarehouse> kc = kcbiz.findKcByRk(cName, brandName, typeName, pModel, firmName);
                            if (kc.size()==0){
                                throw new RuntimeException();
                            }else {
                                KcWarehouse kcWarehouse = kc.get(0);//得到第一个库存
                                wmoney=-wmoney;
                                wbiz.updatemoneyById(ckWarehouse.getWarehouseId(),wmoney,money);
                            }
                        }
                    }
                }
                //确定回款  仓库金额加
                if (state.equals("3")){
                    for (Orderdetails od:orderdetails) {
                        brandName = od.getBrand().getBrandId();
                        typeName=od.getType().getTypeId();
                        pModel=od.getProduct().getProductId();
                        count=od.getPurchaseNum();
                        wmoney=od.getPrototal().doubleValue();
                        List<KcWarehouse> kc = kcbiz.findKcByRk(cName, brandName, typeName, pModel, firmName);
                        KcWarehouse kcWarehouse = kc.get(0);//得到第一个库存
                        System.out.println("库存id    "+kcWarehouse.getId());
                        wbiz.updatemoneyById(ckWarehouse.getWarehouseId(),wmoney,money);
                    }
                }
                Boolean flas = ckbiz.updateStateByid(id, state);
            }catch (Exception e){
                e.printStackTrace();
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }

        return "forward:/storage/delivery/cklist?pageNo="+pageNo1;
    }
}
