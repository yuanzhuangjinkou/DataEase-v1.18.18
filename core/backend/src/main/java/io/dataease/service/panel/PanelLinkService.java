package io.dataease.service.panel;

import io.dataease.auth.config.RsaProperties;
import io.dataease.auth.util.JWTUtils;
import io.dataease.auth.util.RsaUtil;
import io.dataease.commons.constants.SysLogConstants;
import io.dataease.commons.utils.AuthUtils;
import io.dataease.commons.utils.CodingUtil;
import io.dataease.commons.utils.DeLogUtils;
import io.dataease.commons.utils.ServletUtils;
import io.dataease.controller.request.panel.link.*;
import io.dataease.dto.panel.PanelGroupDTO;
import io.dataease.dto.panel.link.GenerateDto;
import io.dataease.dto.panel.link.TicketDto;
import io.dataease.ext.ExtPanelGroupMapper;
import io.dataease.ext.ExtPanelLinkMapper;
import io.dataease.plugins.common.base.domain.*;
import io.dataease.plugins.common.base.mapper.*;
import io.dataease.plugins.common.exception.DataEaseException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Service
public class PanelLinkService {

    private static final String BASEURL = "/link.html?link=";
    private static final String USERPARAM = "&user=";
    private static final String SHORT_URL_PREFIX = "/link/";

    @Value("${server.servlet.context-path:#{null}}")
    private String contextPath;

    @Resource
    private PanelLinkMapper mapper;
    @Resource
    private PanelGroupMapper panelGroupMapper;
    @Resource
    private ExtPanelLinkMapper extPanelLinkMapper;
    @Resource
    private PanelLinkMappingMapper panelLinkMappingMapper;
    @Resource
    private PanelWatermarkMapper panelWatermarkMapper;
    @Resource
    private ExtPanelGroupMapper extPanelGroupMapper;

    @Resource
    private PanelLinkTicketMapper panelLinkTicketMapper;

    @Transactional
    public void changeValid(LinkRequest request) {
        PanelLink po = new PanelLink();
        po.setResourceId(request.getResourceId());
        po.setValid(request.isValid());
        Long userId = AuthUtils.getUser().getUserId();
        mapper.updateByExampleSelective(po, example(request.getResourceId(), userId));

        PanelLinkMappingExample example = new PanelLinkMappingExample();
        example.createCriteria().andResourceIdEqualTo(request.getResourceId()).andUserIdEqualTo(userId);
        PanelLinkMapping mapping = new PanelLinkMapping();
        mapping.setUuid(CodingUtil.shortUuid());
        panelLinkMappingMapper.updateByExampleSelective(mapping, example);
        PanelGroupWithBLOBs panel = panelGroupMapper.selectByPrimaryKey(request.getResourceId());

        SysLogConstants.OPERATE_TYPE operateType = SysLogConstants.OPERATE_TYPE.CREATELINK;
        if (!request.isValid()) {
            operateType = SysLogConstants.OPERATE_TYPE.DELETELINK;
        }
        DeLogUtils.save(operateType, SysLogConstants.SOURCE_TYPE.PANEL, panel.getId(), panel.getPid(), null, null);
    }

    private PanelLinkExample example(String panelLinkId, Long userId) {
        PanelLinkExample example = new PanelLinkExample();
        example.createCriteria().andResourceIdEqualTo(panelLinkId).andUserIdEqualTo(userId);
        return example;
    }

    public void changeEnablePwd(EnablePwdRequest request) {
        PanelLink po = new PanelLink();
        po.setResourceId(request.getResourceId());
        po.setEnablePwd(request.isEnablePwd());
        mapper.updateByExampleSelective(po, example(request.getResourceId(), AuthUtils.getUser().getUserId()));
        PanelGroupWithBLOBs panel = panelGroupMapper.selectByPrimaryKey(request.getResourceId());
        DeLogUtils.save(SysLogConstants.OPERATE_TYPE.MODIFYLINK, SysLogConstants.SOURCE_TYPE.PANEL, panel.getId(), panel.getPid(), null, null);
    }

    public void password(PasswordRequest request) {
        PanelLink po = new PanelLink();
        po.setResourceId(request.getResourceId());
        po.setPwd(request.getPassword());
        mapper.updateByExampleSelective(po, example(request.getResourceId(), AuthUtils.getUser().getUserId()));


        PanelGroupWithBLOBs panel = panelGroupMapper.selectByPrimaryKey(request.getResourceId());
        DeLogUtils.save(SysLogConstants.OPERATE_TYPE.MODIFYLINK, SysLogConstants.SOURCE_TYPE.PANEL, panel.getId(), panel.getPid(), null, null);
    }

    public void overTime(OverTimeRequest request) {
        request.setUserId(AuthUtils.getUser().getUserId());
        extPanelLinkMapper.updateOverTime(request);
        PanelGroupWithBLOBs panel = panelGroupMapper.selectByPrimaryKey(request.getResourceId());
        DeLogUtils.save(SysLogConstants.OPERATE_TYPE.MODIFYLINK, SysLogConstants.SOURCE_TYPE.PANEL, panel.getId(), panel.getPid(), null, null);
    }

    private PanelLink findOne(String resourceId) {
        PanelLinkExample example = new PanelLinkExample();
        example.createCriteria().andResourceIdEqualTo(resourceId).andUserIdIsNull();
        List<PanelLink> list = mapper.selectByExample(example);
        return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
    }

    public PanelLink findOne(String resourceId, Long userId) {
        if (userId == null) {
            return findOne(resourceId);
        }
        List<PanelLink> panelLinks = mapper.selectByExample(example(resourceId, userId));
        if (CollectionUtils.isNotEmpty(panelLinks)) {
            return panelLinks.get(0);
        } else {
            return null;
        }
    }

    public PanelLinkMapping getMapping(PanelLink link) {
        String resourceId = link.getResourceId();
        Long userId = link.getUserId();
        PanelLinkMappingExample example = new PanelLinkMappingExample();
        example.createCriteria().andResourceIdEqualTo(resourceId).andUserIdEqualTo(userId);
        List<PanelLinkMapping> mappings = panelLinkMappingMapper.selectByExample(example);
        if (CollectionUtils.isNotEmpty(mappings)) return mappings.get(0);
        return null;
    }

    public List<PanelLinkTicket> queryTicket(String resourceId) {
        Long userId = AuthUtils.getUser().getUserId();
        PanelLinkMappingExample example = new PanelLinkMappingExample();
        example.createCriteria().andResourceIdEqualTo(resourceId).andUserIdEqualTo(userId);
        List<PanelLinkMapping> mappings = panelLinkMappingMapper.selectByExample(example);
        PanelLinkMapping mapping = mappings.get(0);
        String uuid = mapping.getUuid();
        PanelLinkTicketExample exampleTicket = new PanelLinkTicketExample();
        exampleTicket.createCriteria().andUuidEqualTo(uuid);
        return panelLinkTicketMapper.selectByExample(exampleTicket);
    }

    @Transactional
    public GenerateDto currentGenerate(String resourceId) {
        PanelLink one = findOne(resourceId, AuthUtils.getUser().getUserId());
        if (ObjectUtils.isEmpty(one)) {
            one = new PanelLink();
            one.setPwd(null);
            one.setResourceId(resourceId);
            one.setValid(false);
            one.setUserId(AuthUtils.getUser().getUserId());
            one.setEnablePwd(false);
            mapper.insert(one);
        }
        PanelLinkMappingExample example = new PanelLinkMappingExample();
        example.createCriteria().andResourceIdEqualTo(resourceId).andUserIdEqualTo(AuthUtils.getUser().getUserId());
        List<PanelLinkMapping> mappings = panelLinkMappingMapper.selectByExample(example);
        PanelLinkMapping mapping = null;
        if (CollectionUtils.isEmpty(mappings)) {
            mapping = new PanelLinkMapping();
            mapping.setResourceId(resourceId);
            mapping.setUserId(AuthUtils.getUser().getUserId());
            mapping.setUuid(CodingUtil.shortUuid());
            mapping.setRequireTicket(false);
            panelLinkMappingMapper.insert(mapping);
        } else {
            mapping = mappings.get(0);
        }
        return convertDto(one, mapping.getUuid(), mapping.getRequireTicket());
    }

    public void deleteByResourceId(String resourceId) {
        PanelLinkExample example = new PanelLinkExample();
        example.createCriteria().andResourceIdEqualTo(resourceId);
        mapper.deleteByExample(example);

        PanelLinkMappingExample mappingExample = new PanelLinkMappingExample();
        mappingExample.createCriteria().andResourceIdEqualTo(resourceId);
        panelLinkMappingMapper.deleteByExample(mappingExample);
    }

    public String decryptParam(String text) throws Exception {
        return RsaUtil.decryptByPrivateKey(RsaProperties.privateKey, text);
    }

    // 使用公钥加密
    private String encrypt(String sourceValue) {
        try {
            return RsaUtil.encryptByPublicKey(RsaProperties.publicKey, sourceValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String buildLinkParam(PanelLink link, String uuid) {
        String resourceId = link.getResourceId();
        if (StringUtils.isNotBlank(uuid)) {
            resourceId += ("," + uuid);
        }
        String linkParam = encrypt(resourceId);
        if (link.getUserId() != null) {
            linkParam = linkParam + USERPARAM + encrypt(link.getUserId().toString());
        }
        return linkParam;
    }

    private GenerateDto convertDto(PanelLink link, String uuid, boolean requireTicket) {
        GenerateDto result = new GenerateDto();
        result.setValid(link.getValid());
        result.setEnablePwd(link.getEnablePwd());
        result.setPwd(link.getPwd());
        result.setUri(BASEURL + buildLinkParam(link, uuid));
        result.setOverTime(link.getOverTime());
        result.setRequireTicket(requireTicket);
        return result;
    }

    // 验证请求头部携带的信息 如果正确说明通过密码验证 否则没有通过
    public Boolean validateHeads(PanelLink panelLink) throws Exception {
        HttpServletRequest request = ServletUtils.request();
        String token = request.getHeader("LINK-PWD-TOKEN");
        if (!panelLink.getEnablePwd() || StringUtils.isEmpty(token) || StringUtils.equals("undefined", token)
                || StringUtils.equals("null", token)) {
            String resourceId = panelLink.getResourceId();
            String pwd = "dataease";
            String tk = JWTUtils.signLink(resourceId, panelLink.getUserId(), pwd);
            HttpServletResponse httpServletResponse = ServletUtils.response();
            httpServletResponse.addHeader("Access-Control-Expose-Headers", "LINK-PWD-TOKEN");
            httpServletResponse.setHeader("LINK-PWD-TOKEN", tk);
            return false;
        }
        if (StringUtils.isEmpty(panelLink.getPwd()))
            return false;
        return JWTUtils.verifyLink(token, panelLink.getResourceId(), panelLink.getUserId(), panelLink.getPwd());
    }

    // 验证链接是否过期
    public Boolean isExpire(PanelLink panelLink) {
        if (ObjectUtils.isEmpty(panelLink.getOverTime())) {
            return false;
        }
        return System.currentTimeMillis() > panelLink.getOverTime();
    }

    public boolean validatePwd(PasswordRequest request) throws Exception {
        String password = request.getPassword();
        String resourceId = request.getResourceId();
        PanelLink one = findOne(resourceId, request.getUser());
        String pwd = one.getPwd();
        boolean pass = StringUtils.equals(pwd, password);
        if (pass) {
            String token = JWTUtils.signLink(resourceId, request.getUser(), password);
            HttpServletResponse httpServletResponse = ServletUtils.response();
            httpServletResponse.addHeader("Access-Control-Expose-Headers", "LINK-PWD-TOKEN");
            httpServletResponse.setHeader("LINK-PWD-TOKEN", token);
        }
        return pass;
    }

    public PanelGroupDTO resourceInfo(String resourceId, String userId) {
        PanelGroupDTO result = extPanelGroupMapper.findOneWithPrivileges(resourceId, userId);
        result.setWatermarkInfo(panelWatermarkMapper.selectByPrimaryKey("system_default"));
        return result;
    }

    public String getShortUrl(String resourceId) {
        PanelLinkMappingExample example = new PanelLinkMappingExample();
        example.createCriteria().andResourceIdEqualTo(resourceId).andUserIdEqualTo(AuthUtils.getUser().getUserId());
        List<PanelLinkMapping> mappings = panelLinkMappingMapper.selectByExample(example);
        PanelLinkMapping mapping = mappings.get(0);
        String uuid = mapping.getUuid();
        return contextPath + SHORT_URL_PREFIX + (StringUtils.isBlank(uuid) ? mapping.getId() : uuid);
    }

    public String saveTicket(TicketCreator creator) {
        String ticket = creator.getTicket();
        if (StringUtils.isNotBlank(ticket)) {
            PanelLinkTicket ticketEntity = getByTicket(ticket);
            if (ObjectUtils.isNotEmpty(ticketEntity)) {
                PanelLinkTicketExample example = new PanelLinkTicketExample();
                example.createCriteria().andTicketEqualTo(ticket);
                if (creator.isGenerateNew()) {
                    ticketEntity.setAccessTime(null);
                    ticketEntity.setTicket(CodingUtil.shortUuid());
                }
                ticketEntity.setArgs(creator.getArgs());
                ticketEntity.setExp(creator.getExp());
                ticketEntity.setUuid(creator.getUuid());
                panelLinkTicketMapper.updateByExample(ticketEntity, example);
                return ticketEntity.getTicket();
            }
        }
        ticket = CodingUtil.shortUuid();
        PanelLinkTicket linkTicket = new PanelLinkTicket();
        linkTicket.setTicket(ticket);
        linkTicket.setArgs(creator.getArgs());
        linkTicket.setExp(creator.getExp());
        linkTicket.setUuid(creator.getUuid());
        panelLinkTicketMapper.insert(linkTicket);
        return ticket;
    }

    public void deleteTicket(TicketDelRequest request) {
        String ticket = request.getTicket();
        if (StringUtils.isBlank(ticket)) {
            DataEaseException.throwException("ticket为必填参数");
        }
        PanelLinkTicketExample example = new PanelLinkTicketExample();
        example.createCriteria().andTicketEqualTo(ticket);
        panelLinkTicketMapper.deleteByExample(example);
    }

    public void switchRequire(TicketSwitchRequest request) {
        String resourceId = request.getResourceId();
        Boolean require = request.getRequire();
        PanelLinkMappingExample example = new PanelLinkMappingExample();
        example.createCriteria().andResourceIdEqualTo(resourceId).andUserIdEqualTo(AuthUtils.getUser().getUserId());
        PanelLinkMapping mapping = new PanelLinkMapping();
        mapping.setRequireTicket(require);
        panelLinkMappingMapper.updateByExampleSelective(mapping, example);
    }

    public PanelLinkTicket getByTicket(String ticket) {
        PanelLinkTicketExample example = new PanelLinkTicketExample();
        example.createCriteria().andTicketEqualTo(ticket);
        List<PanelLinkTicket> tickets = panelLinkTicketMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(tickets)) return null;
        return tickets.get(0);
    }

    public String getUrlByIndex(Long index) {
        PanelLinkMapping mapping = panelLinkMappingMapper.selectByPrimaryKey(index);

        String resourceId = mapping.getResourceId();
        Long userId = mapping.getUserId();
        PanelLink one = findOne(resourceId, userId);
        if (StringUtils.isNotBlank(mapping.getUuid())) {
            one.setResourceId("error-resource-id");
        }
        return convertDto(one, mapping.getUuid(), mapping.getRequireTicket()).getUri();
    }

    public String getUrlByUuid(String uuid) {
        PanelLinkMappingExample example = new PanelLinkMappingExample();
        example.createCriteria().andUuidEqualTo(uuid);
        List<PanelLinkMapping> mappings = panelLinkMappingMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(mappings)) {
            PanelLink panelLink = new PanelLink();
            panelLink.setResourceId("error-resource-id");
            return BASEURL + buildLinkParam(panelLink, null);
        }
        PanelLinkMapping mapping = mappings.get(0);
        String resourceId = mapping.getResourceId();
        Long userId = mapping.getUserId();
        PanelLink one = findOne(resourceId, userId);
        return convertDto(one, uuid, mapping.getRequireTicket()).getUri();
    }

    public TicketDto validateTicket(String ticket, PanelLinkMapping mapping) {
        String uuid = mapping.getUuid();
        TicketDto ticketDto = new TicketDto();
        if (StringUtils.isBlank(ticket)) {
            ticketDto.setTicketValid(!mapping.getRequireTicket());
            return ticketDto;
        }
        PanelLinkTicketExample example = new PanelLinkTicketExample();
        example.createCriteria().andTicketEqualTo(ticket).andUuidEqualTo(uuid);
        List<PanelLinkTicket> tickets = panelLinkTicketMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(tickets)) {
            ticketDto.setTicketValid(false);
            return ticketDto;
        }
        PanelLinkTicket linkTicket = tickets.get(0);
        ticketDto.setTicketValid(true);
        ticketDto.setArgs(linkTicket.getArgs());
        Long accessTime = linkTicket.getAccessTime();
        long now = System.currentTimeMillis();
        if (ObjectUtils.isEmpty(accessTime)) {
            accessTime = now;
            ticketDto.setTicketExp(false);
            linkTicket.setAccessTime(accessTime);
            panelLinkTicketMapper.updateByPrimaryKey(linkTicket);
            return ticketDto;
        }
        Long exp = linkTicket.getExp();
        if (ObjectUtils.isEmpty(exp) || exp.equals(0L)) {
            ticketDto.setTicketExp(false);
            return ticketDto;
        }
        long expTime = exp * 60L * 1000L;
        long time = now - accessTime;
        ticketDto.setTicketExp(time > expTime);
        return ticketDto;
    }
}
