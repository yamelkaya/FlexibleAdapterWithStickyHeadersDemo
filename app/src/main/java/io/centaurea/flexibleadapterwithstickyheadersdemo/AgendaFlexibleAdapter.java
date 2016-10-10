package io.centaurea.flexibleadapterwithstickyheadersdemo;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.flexibleadapter.items.IHeader;

public class AgendaFlexibleAdapter extends FlexibleAdapter{

    public static final int BATCH_LIMIT = 100;
    public static final int BATCH_LIMIT_CENTER = BATCH_LIMIT / 2;
    private TaskDbProvider taskProvider;
    private static AgendaListItemComparator comparator = new AgendaListItemComparator();

    public AgendaFlexibleAdapter(TaskDbProvider taskProvider) {
        this(new ArrayList<>(), taskProvider);
    }

    private AgendaFlexibleAdapter(@NonNull List<AgendaListItemVM> items, TaskDbProvider taskProvider) {
        super(items);
        //setAnimationOnScrolling(true);
        setDisplayHeadersAtStartUp(true);
        enableStickyHeaders();

        this.taskProvider = taskProvider;
    }

    public void reloadItems(DateTime batchStartDate) {
        removeAll();

        loadItemsFromDate(batchStartDate.toDate(), BATCH_LIMIT_CENTER, true);
        loadItemsFromDate(batchStartDate.toDate(), -BATCH_LIMIT_CENTER, false);
    }

    public void loadItems(DateTime date) {
        //if adapter has header with required date
        // 1) refresh bounds (trying to keep header with specified date in center)
        // 2) return header position in refreshed item set
        if (isDateLoaded(date)) {
            int oldHeaderPosition = getHeaderPositionByDate(date);
            loadItemsFromPosition(oldHeaderPosition);
        }
        //if adapter does not have header with required date
        // 1) load the whole page (with specified date in center)
        // 2) try to get header position again in new item set
        else {
            reloadItems(date);
        }
    }

    public int getGlobalPositionOfHeader(DateTime date) {
        int position = getHeaderPositionByDate(date);
        return position != -1 ? position : getNearestHeaderPositionByDate(date);
    }

    public DateTime getItemDateTime(int position) {
        if (position < 0) return null;

        return getItemDateTime(getItem(position));
    }

    public DateTime getItemDateTime(IFlexible item) {
        if (item instanceof AgendaListItemVM) {
            return ((AgendaListItemVM) item).getStartDateTime();
        } else {
            return ((AgendaListHeaderVM) item).getDateTime();
        }
    }

    public int calculatePositionFor(AgendaListItemVM item) {
        return calculatePositionFor(item,comparator);
    }

    private int getNearestHeaderPositionByDate(DateTime date) {
        AgendaListHeaderVM lastHeaderDate = getLastHeader();
        AgendaListHeaderVM firstHeaderDate = getFirstHeader();

        if (lastHeaderDate != null && firstHeaderDate != null) {
            int diffInDaysLast = Math.abs(Days.daysBetween(date, lastHeaderDate.getDateTime()).getDays());
            int diffInDaysFirst = Math.abs(Days.daysBetween(date, firstHeaderDate.getDateTime()).getDays());

            return diffInDaysFirst < diffInDaysLast ? getGlobalPositionOf(firstHeaderDate) : getGlobalPositionOf(lastHeaderDate);
        } else return -1;
    }

    private int getHeaderPositionByDate(DateTime date) {
        AgendaListHeaderVM header = getHeaderByDate(date);
        return header != null ? getGlobalPositionOf(header) : -1;
    }

    private AgendaListHeaderVM getHeaderByDate(DateTime date) {
        AgendaListHeaderVM header = null;
        DateTime startOfDate = date.withTimeAtStartOfDay();
        List<AgendaListHeaderVM> tasksScheduledToDate = (List<AgendaListHeaderVM>) Stream.of(getHeaderItems()).filter(i -> ((AgendaListHeaderVM) i).getDateTime().equals(startOfDate)).collect(Collectors.toList());

        if (tasksScheduledToDate.size() > 0) {
            header = tasksScheduledToDate.get(0);
        }
        return header;
    }

    private void loadItemsFromPosition(int position) {
        int offsetFromCenter = getTasksOffsetFromCenter(position);

        if (offsetFromCenter > 0) {
            loadItemsFromDate(getLastItemDate().toDate(), offsetFromCenter, false);
        }
        if (offsetFromCenter < 0) {
            loadItemsFromDate(getFirstItemDate().toDate(), offsetFromCenter, false);
        }
    }

    private boolean isDateLoaded(DateTime date) {
        DateTime startDate = getFirstItemDate();
        DateTime endDate = getLastItemDate();

        return (date.isAfter(startDate) && date.isBefore(endDate)) || date.isEqual(startDate) || date.isEqual(endDate);
    }

    private List<AgendaListItemVM> generateAgendaListItems(List<ScheduleItem> items) {
        List<AgendaListItemVM> data = new ArrayList<>();
        Map<String, AgendaListHeaderVM> headers = new HashMap<>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < items.size(); i++) {
            ScheduleItem item = items.get(i);
            String dateKey = format.format(item.getStartTime());

            AgendaListHeaderVM header = getHeaderByDate(new DateTime(item.getStartTime()));
            if (header == null) {
                header = headers.get(dateKey);

                if (header == null) {
                    header = new AgendaListHeaderVM(item.getStartTime(), false);
                    headers.put(dateKey, header);
                }
            }

            data.add(new AgendaListItemVM(header, item, false));
        }

        return data;
    }

    private void loadItemsFromDate(Date date, int offset, boolean equal) {
        List<ScheduleItem> tasksPage = taskProvider.getTasksPage(new DateTime(date), offset, equal);

        updateItems(tasksPage, offset);
        insertEmptySections();
    }

    private void insertEmptySections() {
        List<AgendaListHeaderVM> headers = getHeaderItems();
        for (int i = 0; i < headers.size() - 1; i++) {
            AgendaListHeaderVM header = headers.get(i);
            AgendaListHeaderVM nextHeader = headers.get(i + 1);

            int diffInDays = Days.daysBetween(header.getDateTime(), nextHeader.getDateTime()).getDays();

            for (int j = 1; j < diffInDays; j++) {
                //TODO: add empty item is a workaround - actually we need just empty section.
                AgendaListItemVM empty = AgendaListItemVM.getEmpty(header.getDateTime().plusDays(j));
                addItem(calculatePositionFor(empty),empty);
                //addSection(new AgendaListHeaderVM(header.getStartOfDayDateTime().plusDays(j).toDate(),true),comparator);
            }
        }
    }

    private void updateItems(List<ScheduleItem> tasksPage, int offset) {
        List<AgendaListItemVM> newItems = generateAgendaListItems(tasksPage);

        if (getItemCount() > 0) {
            addItems(newItems);

            //TODO:PERF - remove items at the opposite side if there will be problems with performance
            int itemsToRemove = getRemoveTasksCount();

            if (offset > 0) {
                removeItemsStartingFrom(0, itemsToRemove, 1);
            } else {
                removeItemsStartingFrom(getItemCount() - 1, itemsToRemove, -1);
            }

            System.out.println("!!!!!!!!!!!!!!!!!!!!!!TASK NUMBER : " + getTaskCount());
        } else {
            addItems(newItems);
        }
    }

    private DateTime getFirstItemDate() {
        if (getTaskCount() == 0) return null;

        List<AgendaListHeaderVM> headers = getHeaderItems();
        for (AgendaListHeaderVM header : headers) {
            List<AgendaListItemVM> items = getSectionItems(header);

            if (items.size() > 0) return items.get(0).getStartDateTime();
        }

        return null;
    }

    private DateTime getLastItemDate() {
        if (getTaskCount() == 0) return null;

        ArrayList<AgendaListHeaderVM> headers = new ArrayList<>(getHeaderItems());
        Collections.reverse(headers);

        for (AgendaListHeaderVM header : headers) {
            List<AgendaListItemVM> items = getSectionItems(header);

            if (items.size() > 0) return items.get(items.size() - 1).getStartDateTime();
        }

        return null;
    }

    private AgendaListHeaderVM getLastHeader() {
        return ((AgendaListHeaderVM) getSectionHeader(getItemCount() - 1));
    }

    private AgendaListHeaderVM getFirstHeader() {
        return ((AgendaListHeaderVM) getSectionHeader(0));
    }

    private int getTasksOffsetFromCenter(int position) {
        int tasksBefore = getTaskCountUntil(position);
        return tasksBefore - BATCH_LIMIT_CENTER;
    }

    private int getTaskCount() {
        return getItemCountOfTypes(R.layout.view_agenda_item);
    }

    private int getTaskCountUntil(int position) {
        return getItemCountOfTypesUntil(position, R.layout.view_agenda_item);
    }

    private int getRemoveTasksCount() {
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!REMOVE TASK COUNT : " + getTaskCount() + " - " + BATCH_LIMIT + " = " + (getTaskCount() - BATCH_LIMIT));
        return getTaskCount() - BATCH_LIMIT;
    }

    private void removeAll() {
        removeRange(0, getItemCount());
    }

    private void removeItemsStartingFrom(int position, int count, int step) {
        if (count <= 0) return;

        int itemsRemoved = 0;
        int index = position;
        List<Integer> indexesToRemove = new ArrayList<>();
        HashSet<IHeader> headersToRemove = new HashSet<>();

        while (itemsRemoved < count && (index != -1 || index != getItemCount())) {
            IFlexible item = getItem(index);

            if (!isHeader(item)) {
                indexesToRemove.add(index);
                itemsRemoved++;
                headersToRemove.add(getHeaderOf(item));
            } else {
                headersToRemove.add((IHeader) item);
            }

            index += step;
        }

        removeItems(indexesToRemove);

        for (IHeader header : headersToRemove) {
            if (getSectionItems(header).size() == 0) {
                removeItem(getGlobalPositionOf(header));
            }
        }
    }

    private void addItems(List<AgendaListItemVM> items) {
        for (AgendaListItemVM item : items) {
            addItem(calculatePositionFor(item),item);
        }
    }

    public static class AgendaListItemComparator implements Comparator<IFlexible> {

        @Override
        public int compare(IFlexible v1, IFlexible v2) {
            int result = 0;
            if (v1 instanceof AgendaListHeaderVM && v2 instanceof AgendaListHeaderVM) {
                result = ((AgendaListHeaderVM) v1).getDateTime().compareTo(((AgendaListHeaderVM) v2).getDateTime());

            } else if (v1 instanceof AgendaListItemVM && v2 instanceof AgendaListItemVM) {
                AgendaListItemVM item1 = (AgendaListItemVM) v1;
                AgendaListItemVM item2 = (AgendaListItemVM) v2;
                result = item1.getHeader().getDateTime().compareTo(item2.getHeader().getDateTime());
                if (result == 0) {
                    result = item1.getStartDateTime().compareTo(item2.getStartDateTime());
                }

            } else if (v1 instanceof AgendaListItemVM && v2 instanceof AgendaListHeaderVM) {
                result = ((AgendaListItemVM) v1).getHeader().getDateTime().compareTo(((AgendaListHeaderVM) v2).getDateTime());
                if (result == 0) result++;

            } else if (v1 instanceof AgendaListHeaderVM && v2 instanceof AgendaListItemVM) {
                result = ((AgendaListHeaderVM) v1).getDateTime().compareTo(((AgendaListItemVM) v2).getHeader().getDateTime());
                if (result == 0) result--;
            }
            return result;
        }
    }
}
