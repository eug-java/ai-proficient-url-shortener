package com.example.shortener.messaging;

import ua_parser.Parser;
import java.io.IOException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClickEventProcessor {
    private final JdbcTemplate jdbc; private final Parser parser;
    public ClickEventProcessor(JdbcTemplate jdbc) throws IOException { this.jdbc=jdbc; this.parser=new Parser(); }
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void process(ClickMessage e) {
        int claimed=jdbc.update("insert into consumer_inbox(event_id,consumer_name,processed_at) values (?,?,now()) on conflict do nothing",
                e.eventId(),"click-rollup-v1");
        if(claimed==0)return;
        var c=parser.parse(e.userAgent()==null?"":e.userAgent());
        String browser=trim(c.userAgent.family), os=trim(c.os.family), device=trim(c.device.family);
        jdbc.update("""
          insert into click_event(id,event_id,organization_id,url_mapping_id,short_code,occurred_at,ip_hash,user_agent,referrer,country_code,device_type,browser,os)
          values (?,?,?,?,?,?,?,?,?,?,?,?,?)
          """,java.util.UUID.randomUUID(),e.eventId(),e.orgId(),e.urlMappingId(),e.shortCode(),
          Timestamp.from(e.occurredAt()),e.ipHash(),e.userAgent(),e.referrer(),e.countryCode(),device,browser,os);
        jdbc.update("update url_mapping set total_clicks=total_clicks+1,last_accessed_at=?,version=version+1 where id=?",
                Timestamp.from(e.occurredAt()),e.urlMappingId());
        jdbc.update("""
          insert into link_stats_total(url_mapping_id,organization_id,short_code,total_clicks,last_clicked_at,unique_ip_hashes)
          values (?,?,?,1,?,?) on conflict(url_mapping_id) do update set total_clicks=link_stats_total.total_clicks+1,
          last_clicked_at=excluded.last_clicked_at,
          unique_ip_hashes=(select count(distinct ip_hash) from click_event where url_mapping_id=excluded.url_mapping_id and ip_hash is not null)
          """,e.urlMappingId(),e.orgId(),e.shortCode(),Timestamp.from(e.occurredAt()),e.ipHash()==null?0:1);
        jdbc.update("""
          insert into link_stats_daily(url_mapping_id,day,organization_id,short_code,clicks,unique_ip_hashes)
          values (?,?::date,?,?,1,?) on conflict(url_mapping_id,day) do update set clicks=link_stats_daily.clicks+1,
          unique_ip_hashes=(select count(distinct ip_hash) from click_event where url_mapping_id=excluded.url_mapping_id and occurred_at::date=excluded.day and ip_hash is not null)
          """,e.urlMappingId(),e.occurredAt().toString().substring(0,10),e.orgId(),e.shortCode(),e.ipHash()==null?0:1);
        dimension(e,"referrer",host(e.referrer())); dimension(e,"browser",browser);
        dimension(e,"os",os); dimension(e,"device",device); dimension(e,"country",trim(e.countryCode()));
    }
    private void dimension(ClickMessage e,String key,String value){
        jdbc.update(""" 
          insert into link_dimension_daily(url_mapping_id,day,dimension,dimension_value,clicks) values (?,?::date,?,?,1)
          on conflict(url_mapping_id,day,dimension,dimension_value) do update set clicks=link_dimension_daily.clicks+1
          """,e.urlMappingId(),e.occurredAt().toString().substring(0,10),key,value);
    }
    private static String host(String s){try{return s==null?"direct":java.net.URI.create(s).getHost();}catch(Exception x){return"invalid";}}
    private static String trim(String s){return s==null||s.isBlank()?"unknown":s.substring(0,Math.min(128,s.length()));}
}
