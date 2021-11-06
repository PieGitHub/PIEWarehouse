package com.erp.storage.controller;

import com.erp.aoplog.MyLog;
import com.erp.bean.*;
import com.erp.storage.biz.KcWarehouseBiz;
import com.erp.storage.biz.RkWarehouseBiz;
import com.erp.storage.biz.WarehouseBiz;
import com.erp.storage.dao.WarehouseDao;
import com.github.pagehelper.PageInfo;
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
import java.util.List;

@EnableTransactionManagement
@Controller
@RequestMapping("/storage/stock")
public class RkWarehouseController {
    @Autowired
    private RkWarehouseBiz rkbiz;
    @Autowired
    private WarehouseDao wdao;
    @Autowired
    private KcWarehouseBiz kcbiz;
    @Autowired
    private WarehouseBiz wbiz;

    //每次执行前会优先 将mid放入request范围
    @ModelAttribute
    public void initModuleData(HttpSession session) {
        session.removeAttribute("mid");
        session.setAttribute("mid", 13);
    }

    @RequestMapping("/rklist")
    @MyLog(value = "查看入库信息列表")
    public String into(HttpSession session,Model model, @RequestParam(value ="pageNo",defaultValue ="1") Integer pageNo, @RequestParam(value ="pageSize",defaultValue ="5") Integer pageSize){
        RkWarehouse rk=new RkWarehouse();
        Users loguser = (Users)session.getAttribute("loguser");
        PageInfo<RkWarehouse> rkPage =null;
        if (loguser.getJobId()==1 || loguser.getJobId()==6){
            rkPage = rkbiz.findAllByPage(null,null, null, null, pageNo, pageSize);
        }else {
            rkPage = rkbiz.findAllByPage(loguser.getuId(),null, null, null, pageNo, pageSize);
        }

        List<RkWarehouse> rklist = rkPage.getList();
        model.addAttribute("rkPage",rkPage);
        model.addAttribute("rklist",rklist);
        model.addAttribute("rk",rk);
        return "storage/stock/stockList";
    }
    @RequestMapping("/check")
    @MyLog(value = "条件查询入库列表")
    public String check(HttpSession session,Model model, RkWarehouse rk, @RequestParam(value ="pageNo",defaultValue ="1") Integer pageNo, @RequestParam(value ="pageSize",defaultValue ="5") Integer pageSize){
        Users loguser = (Users)session.getAttribute("loguser");
        PageInfo<RkWarehouse> rkPage =null;
        if (loguser.getJobId()==1 || loguser.getJobId()==6){
            rkPage = rkbiz.findAllByPage(null,rk.getWarehouse().getcName(), rk.getRkIndent(), rk.getState(), pageNo, pageSize);
        }else {
            rkPage = rkbiz.findAllByPage(loguser.getuId(),rk.getWarehouse().getcName(), rk.getRkIndent(), rk.getState(), pageNo, pageSize);
        }
        List<RkWarehouse> rklist = rkPage.getList();
        model.addAttribute("rkPage",rkPage);
        model.addAttribute("rklist",rklist);
        model.addAttribute("rk",rk);
        return "storage/stock/stockList";
    }

    /**
     * 入库详情
     * @param rkIndent
     * @return
     */
    @RequestMapping("/view/{rkIndent}")
    @MyLog(value = "查看入库详情")
    public String view(@PathVariable("rkIndent") String rkIndent,Model model){
        RkWarehouse rk = rkbiz.findRkById(rkIndent);
        List<CDetails> detailsById = rkbiz.findDetailsById(rkIndent);
        model.addAttribute("detailsById",detailsById);
        model.addAttribute("rk",rk);
        return "storage/stock/stockView";
    }

    /**
     * 跳转添加页面
     * @return
     */
    @RequestMapping("/goadd")
    public String goadd(Model model){
        List<Warehouse> warehouse = wdao.findAllBystate1();
        List<String>  indentlist= rkbiz.findIndentnotinrk();
        model.addAttribute("indentlist",indentlist);
        model.addAttribute("warehouselist",warehouse);
        return "storage/stock/stockAdd";
    }

    /**
     * 添加入库信息
     * @param rk
     * @return
     */
    @RequestMapping("/add")
    @MyLog(value = "添加入库信息")
    public String add(RkWarehouse rk){
        int i = rkbiz.insertSelective(rk);
        return "forward:/storage/stock/rklist";
    }

    /**
     * 入库，取消入库
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @RequestMapping("/revese")
    @MyLog(value = "取消入库、入库")
    public String revese(@RequestParam("id") Integer id,@RequestParam("state") String state,@RequestParam("pageNo1") Integer pageNo1){
        System.out.println(id);
        System.out.println(state);

            RkWarehouse rkWarehouse = rkbiz.selectByPrimaryKey(id);//根据入库id查询入库信息
            System.out.println("仓库id："+rkWarehouse.getWarehouseId());//仓库id
            System.out.println("入库订单id："+rkWarehouse.getRkIndent());//入库订单
            RkWarehouse rk = rkbiz.findRkById(rkWarehouse.getRkIndent());//入库订单查询入库详情
            List<CDetails> details = rkbiz.findDetailsById(rkWarehouse.getRkIndent());
            Integer cName= rk.getWarehouse().getId();//仓库名
            Integer brandName=null;//品牌id
            Integer typeName=null;//类型id
            Integer pModel=null;//型号id
            Integer firmName=null;//厂商名
            Integer count=null;//数量
            Double wmoney=null;//修改金额
            Double money=rk.getWarehouse().getwMoney();//仓库总金额
        System.out.println("====================="+money);
            try {
                //取消入库  库存减少  仓库金额增加
                if (state.equals("1")){
                    for (CDetails cd:details) {
                        brandName = cd.getcBrand().getBrandId();
                        typeName=cd.getcType().getTypeId();
                        pModel=cd.getcProduct().getProductId();
                        firmName=cd.getcFirm().getFirmId();
                        count=cd.getCount();
                        wmoney=cd.getTotalMoney().doubleValue();
                        List<KcWarehouse> kc = kcbiz.findKcByRk(cName, brandName, typeName, pModel, firmName);
                        if (kc.size()==0){
                            throw new Exception();
                        }
                        KcWarehouse kcWarehouse = kc.get(0);//得到第一个库存
                        System.out.println("库存id    "+kcWarehouse.getId());
                        count=-count;
                        kcbiz.updateKcRepertoryById(kcWarehouse.getId(),count);
                        wbiz.updatemoneyById(rkWarehouse.getWarehouseId(),wmoney,money);
                    }
                }
                //入库  库存增加  仓库金额减少
                if (state.equals("2")){
                    for (CDetails cd:details) {
                        brandName = cd.getcBrand().getBrandId();
                        typeName=cd.getcType().getTypeId();
                        pModel=cd.getcProduct().getProductId();
                        firmName=cd.getcFirm().getFirmId();
                        count=cd.getCount();
                        wmoney=cd.getTotalMoney().doubleValue();
                        List<KcWarehouse> kc = kcbiz.findKcByRk(cName, brandName, typeName, pModel, firmName);
                        System.out.println("--------------------------------------------------"+kc+"--------------------"+kc.size());

                        KcWarehouse kcWarehouse=null;
                        if (kc.size()!=0){
                            kcWarehouse = kc.get(0);//得到第一个库存
                            System.out.println("库存id    "+kcWarehouse.getId());
                            kcbiz.updateKcRepertoryById(kcWarehouse.getId(),count);
                        }else {
                            kcbiz.insertByKc(cName, brandName, typeName, pModel, firmName,count);
                        }

                        wmoney=-wmoney;
                        wbiz.updatemoneyById(rkWarehouse.getWarehouseId(),wmoney,money);
                    }
                }
                Boolean flas = rkbiz.updateStateById(id, state);
            }catch (Exception e){
                e.printStackTrace();
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
        return "forward:/storage/stock/rklist?pageNo="+pageNo1;
    }
}
