/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.openidentityplatform.openam.click.control;

import org.openidentityplatform.openam.click.util.HtmlStringBuffer;

/**
 * Provides the default Table Paginator.
 *
 * <table class='htmlHeader' cellspacing='10'>
 * <tr>
 * <td>
 * <img align='middle' hspace='2'src='default-paginator.png' title='Table'/>
 * </td>
 * </tr>
 * </table>
 */
public class TablePaginator implements Renderable {

    private static final long serialVersionUID = 1L;

    /** The parent table to provide paginator for. */
    protected Table table;

    // Constructors -----------------------------------------------------------

    /**
     * Create a Paginator for the given Table.
     *
     * @param table the paginator's table
     */
    public TablePaginator(Table table) {
        setTable(table);
    }

    // Public Methods ---------------------------------------------------------

    /**
     * Return the parent Table for this Paginator.
     *
     * @return the paginator's parent table
     */
    public Table getTable() {
        return table;
    }

    /**
     * Set the parent Table for this Paginator.
     *
     * @param table the paginator's parent table
     */
    public void setTable(Table table) {
        this.table = table;
    }

    /**
     * @see Renderable#render(HtmlStringBuffer)
     *
     * @param buffer the string buffer to render the paginator to
     */
    public void render(HtmlStringBuffer buffer) {
        final Table table = getTable();

        if (table == null) {
            throw new IllegalStateException("No parent table defined."
                + " Ensure a parent Table is set using #setTable(Table).");
        }

        if (table.getShowBanner()) {
            int rowCount = table.getRowCount();
            String rowCountStr = String.valueOf(rowCount);

            String firstRow = null;
            if (table.getRowList().isEmpty()) {
                firstRow = String.valueOf(0);
            } else {
                firstRow = String.valueOf(table.getFirstRow() + 1);
            }

            String lastRow = null;
            if (table.getRowList().isEmpty()) {
                lastRow = String.valueOf(0);
            } else {
                lastRow = String.valueOf(table.getLastRow());
            }

            Object[] args = { rowCountStr, firstRow, lastRow};

            if (table.getPageSize() > 0) {
                buffer.append(table.getMessage("table-page-banner", args));
            } else {
                buffer.append(table.getMessage("table-page-banner-nolinks", args));
            }
        }

        if (table.getPageSize() > 0) {
            String firstLabel = table.getMessage("table-first-label");
            String firstTitle = table.getMessage("table-first-title");
            String previousLabel = table.getMessage("table-previous-label");
            String previousTitle = table.getMessage("table-previous-title");
            String nextLabel = table.getMessage("table-next-label");
            String nextTitle = table.getMessage("table-next-title");
            String lastLabel = table.getMessage("table-last-label");
            String lastTitle = table.getMessage("table-last-title");
            String gotoTitle = table.getMessage("table-goto-title");

            final ActionLink controlLink = table.getControlLink();

            if (table.getSortedColumn() != null) {
                controlLink.setParameter(org.apache.click.control.Table.SORT, null);
                controlLink.setParameter(org.apache.click.control.Table.COLUMN, table.getSortedColumn());
                controlLink.setParameter(org.apache.click.control.Table.ASCENDING, String.valueOf(table.isSortedAscending()));
            } else {
                controlLink.setParameter(org.apache.click.control.Table.SORT, null);
                controlLink.setParameter(org.apache.click.control.Table.COLUMN, null);
                controlLink.setParameter(org.apache.click.control.Table.ASCENDING, null);
            }

            if (table.getPageNumber() > 0) {
                controlLink.setLabel(firstLabel);
                controlLink.setParameter(org.apache.click.control.Table.PAGE, String.valueOf(0));
                controlLink.setTitle(firstTitle);
                firstLabel = controlLink.toString();

                controlLink.setLabel(previousLabel);
                controlLink.setParameter(org.apache.click.control.Table.PAGE, String.valueOf(table.getPageNumber() - 1));
                controlLink.setTitle(previousTitle);
                previousLabel = controlLink.toString();
            }

            HtmlStringBuffer pagesBuffer =
                new HtmlStringBuffer(table.getNumberPages() * 70);

            // Create sliding window of paging links
            int lowerBound = Math.max(0, table.getPageNumber() - 5);
            int upperBound = Math.min(lowerBound + 10, table.getNumberPages());
            if (upperBound - lowerBound < 10) {
                lowerBound = Math.max(upperBound - 10, 0);
            }

            for (int i = lowerBound; i < upperBound; i++) {
                String pageNumber = String.valueOf(i + 1);
                if (i == table.getPageNumber()) {
                    pagesBuffer.append("<strong>" + pageNumber + "</strong>");

                } else {
                    controlLink.setLabel(pageNumber);
                    controlLink.setParameter(org.apache.click.control.Table.PAGE, String.valueOf(i));
                    controlLink.setTitle(gotoTitle + " " + pageNumber);
                    controlLink.render(pagesBuffer);
                }

                if (i < upperBound - 1) {
                    pagesBuffer.append(", ");
                }
            }
            String pageLinks = pagesBuffer.toString();

            if (table.getPageNumber() < table.getNumberPages() - 1) {
                controlLink.setLabel(nextLabel);
                controlLink.setParameter(org.apache.click.control.Table.PAGE, String.valueOf(table.getPageNumber() + 1));
                controlLink.setTitle(nextTitle);
                nextLabel = controlLink.toString();

                controlLink.setLabel(lastLabel);
                controlLink.setParameter(Table.PAGE, String.valueOf(table.getNumberPages() - 1));
                controlLink.setTitle(lastTitle);
                lastLabel = controlLink.toString();
            }

            Object[] args =
                { firstLabel, previousLabel, pageLinks, nextLabel, lastLabel };

            if (table.getShowBanner()) {
                buffer.append(table.getMessage("table-page-links", args));
            } else {
                buffer.append(table.getMessage("table-page-links-nobanner", args));
            }
            controlLink.setTitle(null);
        }
    }

    /**
     * Returns the HTML representation of this paginator.
     * <p/>
     * This method delegates the rendering to the method
     * {@link #render(HtmlStringBuffer)}.
     *
     * @see Object#toString()
     *
     * @return the HTML representation of this paginator
     */
    @Override
    public String toString() {
        HtmlStringBuffer buffer = new HtmlStringBuffer();

        render(buffer);

        return buffer.toString();
    }

}
