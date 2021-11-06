package com.erp.storage.controller;

import com.erp.aoplog.MyLog;
import com.erp.bean.KcWarehouse;
import com.erp.bean.Users;
import com.erp.storage.biz.KcWarehouseBiz;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/storage/inventory")
public class KcWarehouseController {
    @Autowired
    private KcWarehouseBiz kcbiz;

    //每次执行前会优先 将mid放入request范围
    @ModelAttribute
    public void initModuleData(HttpSession session) {
        session.removeAttribute("mid");
        session.setAttribute("mid", 14);
    }


    /**
     * 初始化库存
     * @param kc
     * @param model
     * @param pageNo
     * @param pageSiz
     * @return
     */
    @RequestMapping("/kclist")
    @MyLog(value = "查看库存列表")
    public String findAllPage(HttpSession session,KcWarehouse kc, Model model, @RequestParam(value = "pageno",defaultValue = "1") Integer pageNo, @RequestParam(value = "pageSiz",defaultValue = "10")Integer pageSiz){
        Users loguser = (Users)session.getAttribute("loguser");
        PageInfo<KcWarehouse> allkcPage =null;
        if (loguser.getJobId()==1 ||loguser.getJobId()==6){
             allkcPage = kcbiz.findAllPage(null,pageNo,pageSiz);
        }else {
            allkcPage = kcbiz.findAllPage(loguser.getuId(),pageNo,pageSiz);
        }
        List<KcWarehouse> kclist = allkcPage.getList();
        model.addAttribute("kc",kc);
        model.addAttribute("kclist",kclist);
        model.addAttribute("kcPage",allkcPage);
        return "storage/inventory/inventoryList";
    }

    /**
     * 根据条件查询库存，分页
     * @param kc  库存对象
     * @param model
     * @param pageNo 页码
     * @param pageSiz 每页大小
     * @return
     */
    @RequestMapping("/check")
    @MyLog(value = "条件查询库存")
    public String findAllkcPage(HttpSession session,KcWarehouse kc,Model model, @RequestParam(value = "pageno",defaultValue = "1") Integer pageNo, @RequestParam(value = "pageSiz",defaultValue = "10")Integer pageSiz){
        Users loguser = (Users)session.getAttribute("loguser");
        PageInfo<KcWarehouse> allkcPage =null;
        if (loguser.getJobId()==1 || loguser.getJobId()==6){
            allkcPage = kcbiz.findAllkcPage(null,kc.getWarehouse().getcName(),kc.getcBrand().getBrandName(),kc.getcType().getTypeName(),kc.getcProduct().getProductModel(),pageNo,pageSiz);
        }else {
            allkcPage = kcbiz.findAllkcPage(loguser.getuId(),kc.getWarehouse().getcName(),kc.getcBrand().getBrandName(),kc.getcType().getTypeName(),kc.getcProduct().getProductModel(),pageNo,pageSiz);
        }
        List<KcWarehouse> kclist = allkcPage.getList();
        model.addAttribute("kc",kc);
        model.addAttribute("kclist",kclist);
        model.addAttribute("kcPage",allkcPage);
        return "storage/inventory/inventoryList";
    }

}
