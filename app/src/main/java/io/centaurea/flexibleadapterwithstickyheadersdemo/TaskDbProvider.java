package io.centaurea.flexibleadapterwithstickyheadersdemo;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by Natasha on 23.07.16.
 */
public class TaskDbProvider {
    public static List<ScheduleItem> tasks;

    TaskDbProvider(){
        tasks = new ArrayList<>();

        DateTime startDate = DateTime.now().minusMonths(12).withTimeAtStartOfDay();
        DateTime end = startDate.plusMonths(24);
        while (startDate.isBefore(end)) {
            for (int i=0; i<3;i++){
                ScheduleItem t =new ScheduleItem();
                t.setTitle(startDate.plusHours(i).toString("yyyy-MM-dd HH:mm"));
                t.setStartTime(startDate.plusHours(i).toDate());
                t.set_id(UUID.randomUUID().toString());

                tasks.add(t);
            }

            startDate = startDate.plusDays(2);
        }
    }

    public List<ScheduleItem> getTasksPage(DateTime batchStartDate, int offset, boolean equal) {
        List<ScheduleItem> result = null;

        if (offset > 0){
            result = Stream.of(tasks).filter(t -> {
                DateTime date = new DateTime(t.getStartTime());
                return date.isAfter(batchStartDate) || (equal ? date.isEqual(batchStartDate) : false);
            }).limit(offset).collect(Collectors.toList());
        }
        if (offset < 0){
            result = Stream.of(tasks).filter(t -> {
                DateTime date = new DateTime(t.getStartTime());
                return date.isBefore(batchStartDate) || (equal ? date.isEqual(batchStartDate) : false);
            }).collect(Collectors.toList());

            Collections.reverse(result);
            result = result.subList(0,Math.min(-offset,result.size()));
            Collections.reverse(result);
        }

        return result;
    }
}
