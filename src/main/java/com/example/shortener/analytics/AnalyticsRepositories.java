package com.example.shortener.analytics;
import java.time.LocalDate; import java.util.List; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
interface ClickEventRepository extends JpaRepository<ClickEvent,UUID>{}
interface LinkStatsTotalRepository extends JpaRepository<LinkStatsTotal,UUID>{}
interface LinkStatsDailyRepository extends JpaRepository<LinkStatsDaily,LinkStatsDaily.Key>{
 List<LinkStatsDaily> findAllByUrlMappingIdAndDayBetweenOrderByDay(UUID id,LocalDate from,LocalDate to);
}
interface LinkDimensionDailyRepository extends JpaRepository<LinkDimensionDaily,LinkDimensionDaily.Key>{
 List<LinkDimensionDaily> findAllByUrlMappingIdAndDayBetweenAndDimension(UUID id,LocalDate from,LocalDate to,String dimension);
}
