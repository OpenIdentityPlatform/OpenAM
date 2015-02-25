/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */
#include <stdlib.h>
#include <ncurses.h>
#include <form.h>
#include <menu.h>
#include <assert.h>
#include <ctype.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <stdio.h>
#include <dlfcn.h>
#include "utils.h"
#include "version.h"
#include "varnish.h"
#include "net.h"

#define BUFSZ 4096
const char name[] = DESCRIPTION;
char vers[64] = "";
const char prev[] = "F2 Previous";
const char next[] = "F3 Next";
const char configure[] = "F3 Configure";
const char quit[] = "F4 Return to menu";
const char cryptm[] = "F2 Encrypt";
const char lexit[] = "F4 Exit";
const char f2accept[] = "F2 Accept";

#define PROFILE_NOT_FOUND "Incorrect Profile ID or Password"

char instance_path[BUFSZ];
char lic_path[BUFSZ];
char log_path[BUFSZ];
char **menu_values;
int menu_size;

char openam_url[BUFSZ];
char agent_url[BUFSZ];
char agent_id[64];
char agent_pass[64];
char agent_pass_file[BUFSZ];
char web_conf_path[BUFSZ];
char web_conf_path_out[BUFSZ];

char int_conf_path[BUFSZ];

int oerr = 0, oidx = 1, oopt, oreset;
char *oarg;

int create_varnish_links(const char *vmp, const char *instp);
void create_varnish_pa_conf(const char *ainstp, url_t url, const char *cafn);
int create_varnish_instance(url_t ua);
void display_usage();
void step(int step, int height, int width);
void encrypt_step(int height, int width);
int remove_step(const char *name, int height, int width);
int remove_instance(const char *agent_instance_name);
int configure_step(int height, int width);
int mainmenu(int height, int width);

void VAS_Fail(const char *func, const char *file, int line,
        const char *cond, int err, int xxx) {
}

const void * const vrt_magic_string_end = &vrt_magic_string_end;

void print_version(FILE *file, char **version) {
    typedef void (*am_agent_version) (char **);
#define DESCRIPTION_EXT DESCRIPTION" for Varnish Cache Server"
    void *lib = NULL;
    void *self = NULL;
    char **v = NULL;
    int builtin = 1;

    self = dlopen(NULL, RTLD_LAZY);
    if (!self) {
#ifdef DEBUG
        fprintf(stderr, "dlopen failed: %s\n", dlerror());
#endif
    }

    if ((lib = dlopen("libvmod_am.so", RTLD_LAZY)) != NULL) {
        am_agent_version f;
        f = (am_agent_version) dlsym(lib, "am_agent_version");
        if (f != NULL) {
            v = (char **) malloc(4 * sizeof (char *));
            if (v != NULL) {
                f(v);
                if (file != NULL) {
                    fprintf(file,
                            "\n%s\n===================================================\nVersion: %s\n%s\nBuild Date: %s\nBuild Machine: %s\n\n",
                            DESCRIPTION_EXT, v[0], v[1], v[2], v[3]);
                }
                builtin = 0;
                if (version != NULL) {
                    *version = strdup(v[0]);
                } else {
                    free(v);
                }
            }
        }
#ifdef DEBUG
        else fprintf(stderr, "dlsym failed: %s\n", dlerror());
#endif
        dlclose(lib);
    }
#ifdef DEBUG
    else fprintf(stderr, "dlopen failed: %s\n", dlerror());
#endif
    if (builtin) {
        if (file != NULL) {
            fprintf(file, "\n%s\n===================================================\n%s\n%s\n%s\n%s\n\n",
                    DESCRIPTION_EXT, VERSION, VERSION_SVN, BUILD_TS, BUILD_SYS);
        }
        if (version != NULL) {
            version = NULL;
        }
    }

    if (self != NULL)
        dlclose(self);
}

int menu_selected(char *name) {
    int i;
    for (i = 0; i < menu_size; ++i) {
        if (strcmp(name, menu_values[i]) == 0) {
            return i;
        }
    }
    return -1;
}

void clean_main_menu() {
    int i;
    if (menu_size > 0) {
        for (i = 0; i < menu_size; ++i) {
            am_free(menu_values[i]);
        }
        am_free(menu_values);
    }
}

void build_main_menu(const char *path) {
    int i = 0, n = 0;
    am_conf_p inst = NULL, temp;
    n = am_read_instances(path, &inst);
    menu_values = (char **) malloc((n + 3) * sizeof (char *));
    menu_size = n + 3;
    menu_values[0] = NULL;
    menu_values[1] = NULL;
    asprintf(&menu_values[0], "Configure "
            "Varnish"
            " Web Policy Agent instance");
    asprintf(&menu_values[1], "Encrypt password");
    i = 2;
    if (n > 0) {
        while (inst != NULL) {
            temp = inst->next;
            menu_values[i] = NULL;
            asprintf(&menu_values[i], "Remove '%s' instance", inst->name);
            inst = temp;
            i++;
        }
    }
    menu_values[i] = NULL;
    asprintf(&menu_values[i], "Exit");
    am_free_conf(inst);
}

WINDOW *license(int height, int width, int *res) {
    WINDOW *win, *lw;
    int i, pr = 0;
    char *lic = NULL;
    win = newwin(height, width, 0, 0);
    keypad(win, TRUE);
    lw = newpad(400, width - 2);
    curs_set(0);
    attron(A_BOLD);
    mvwprintw(win, 0, 0, name);
    mvwprintw(win, 0, width - strlen(vers), vers);
    mvwprintw(win, height - 1, 0, f2accept);
    mvwprintw(win, height - 1, width - strlen(lexit), lexit);
    attroff(A_BOLD);
    wrefresh(win);
    lic = am_read_file(lic_path);
    if (lic == NULL) {
        goto lic_agree;
    } else {
        waddstr(lw, lic);
    }
    prefresh(lw, 0, 0, 2, 1, height - 3, width - 2);
    while ((i = wgetch(win)) != KEY_F(4)) {
        switch (i) {
            case KEY_DOWN:
                pr++;
                pnoutrefresh(lw, pr, 0, 2, 1, height - 3, width - 2);
                break;
            case KEY_UP:
                pr--;
                if (pr < 0) pr = 0;
                pnoutrefresh(lw, pr, 0, 2, 1, height - 3, width - 2);
                break;
            case KEY_F(2):
                goto lic_agree;
        }
        doupdate();
    }
lic_agree:
    curs_set(1);
    wnoutrefresh(win);
    doupdate();
    *res = i;
    if (lic) free(lic);
    return win;
}

int opts(int nargc, char **nargv, const char *ostr) {
    static char *place = EMSG;
    char *oli;
    if (ostr == NULL) return (-1);
    if (oreset || !*place) {
        oreset = 0;
        if (oidx >= nargc || *(place = nargv[oidx]) != '-') {
            place = EMSG;
            return (-1);
        }
        if (place[1] && *++place == '-') {
            ++oidx;
            place = EMSG;
            return (-1);
        }
    }
    if ((oopt = (int) *place++) == (int) ':' || !(oli = strchr(ostr, oopt))) {
        if (oopt == (int) '-')
            return (-1);
        if (!*place)
            ++oidx;
        if (oerr && *ostr != ':')
            (void)fprintf(stderr, "illegal option -- %c\n", oopt);
        return (BADCH);
    }
    if (*++oli != ':') {
        oarg = NULL;
        if (!*place)
            ++oidx;
    } else {
        if (*place)
            oarg = place;
        else if (nargc <= ++oidx) {
            place = EMSG;
            if (*ostr == ':')
                return (BADARG);
            if (oerr)
                (void)fprintf(stderr, "option requires an argument -- %c\n", oopt);
            return (BADCH);
        } else
            oarg = nargv[oidx];
        place = EMSG;
        ++oidx;
    }
    return (oopt);
}

int main(int argc, char** argv) {
    WINDOW *lic = NULL;
    int x, y, mr;
    int opt = 0, cnt = 0;
    char remname[256];
    char *licpath = NULL;
    FILE *fout = NULL;
    char *version = NULL;
    if (am_bin_path(instance_path, sizeof (instance_path)) == -1) {
        return 0;
    } else {
        if (am_whitespace(instance_path) > 0) {
            fprintf(stderr, "Whitespace in path: %s \n", instance_path);
            return 0;
        }

        snprintf(log_path, sizeof (log_path), "%s/../logs/install_%s.log", instance_path, TIMESTAMPLONG);
        snprintf(lic_path, sizeof (lic_path), "%s/../legal-notices/license.txt", instance_path);

        fout = fopen(log_path, "a");
        if (fout != NULL) {
            fprintf(fout, "\n\n===================================================");
            print_version(fout, NULL);
            fprintf(fout, "\r\n");
            fclose(fout);
        }

        strcat(instance_path, "../instances");
        snprintf(int_conf_path, sizeof (int_conf_path), "%s/.agents", instance_path);
    }

    net_initialize();

    /* cli mode */
    if (argc > 1) {
        while ((opt = opts(argc, argv, "e:vlxr:o:a:i:p:c:")) != -1)
            switch (opt) {
                case 'e':
                {
                    char encryptpasswd[1024] = "";
                    char origpasswd[1024] = "";
                    char *keystr;
                    char bkeystr[1024] = "";
                    strcpy(origpasswd, oarg);
                    am_trim(origpasswd);
                    keystr = am_random_key();
                    memset(bkeystr, 0, 1024);
                    am_b64encode(keystr, bkeystr);
                    encrypt_base64(origpasswd, encryptpasswd, bkeystr);
                    fprintf(stderr, "\nEncrypted password:\n%s\n\nKey:\n%s\n\n", encryptpasswd, bkeystr);
                    net_shutdown();
                    return (EXIT_SUCCESS);
                }
                    break;
                case 'l':
                {
                    fprintf(stderr, "Agent instances:\n");
                    int n;
                    am_conf_p inst = NULL, temp;
                    if ((n = am_read_instances(instance_path, &inst)) > 0) {
                        temp = inst;
                        while (temp != NULL) {
                            fprintf(stderr, "%s\n", temp->name);
                            temp = temp->next;
                        }
                        am_free(inst);
                    } else
                        fprintf(stderr, "There are no agent instances registered.\n");
                    net_shutdown();
                    return (EXIT_SUCCESS);
                }
                    break;
                case 'v':
                {
                    print_version(stdout, NULL);
                    net_shutdown();
                    return (EXIT_SUCCESS);
                }
                    break;
                case 'r':
                {
                    fprintf(stderr, "Removing \"%s\" instance...\n", oarg);
                    if (remove_instance(oarg)) {
                        fprintf(stderr, "Instance \"%s\" removed.\n", oarg);
                    } else
                        fprintf(stderr, "Error removing \"%s\" instance.\n", oarg);
                    net_shutdown();
                    return (EXIT_SUCCESS);
                }
                    break;
                case 'o':
                    sprintf(openam_url, oarg);
                    cnt = 1;
                    break;
                case 'a':
                    sprintf(agent_url, oarg);
                    cnt = 1;
                    break;
                case 'i':
                    sprintf(agent_id, oarg);
                    cnt = 1;
                    break;
                case 'p':
                    sprintf(agent_pass_file, oarg);
                    cnt = 1;
                    break;
                case 'c':
                    sprintf(web_conf_path, oarg);
                    cnt = 1;
                    break;
                case 'x':
                {
                    asprintf(&licpath, "%s/.license", instance_path);
                    if (licpath) {
                        am_setup_conf(licpath, NULL);
                        free(licpath);
                        licpath = NULL;
                    }
                    net_shutdown();
                    return (EXIT_SUCCESS);
                }
                    break;
                case '?':
                    if (oopt == 'e' || oopt == 'r' || oopt == 'o' || oopt == 'a' || oopt == 'i' || oopt == 'p' || oopt == 'c')
                        fprintf(stderr, "\nError: option -%c requires an argument.\n", oopt);
                    else if (isprint(oopt))
                        fprintf(stderr, "\nError: unknown option `-%c'.\n", oopt);
                    else
                        fprintf(stderr, "\nnError: unknown option character `\\x%x'.\n", oopt);
                    opt = -1;
                default:
                    opt = -1;
            }

        if (cnt == 1) {
            asprintf(&licpath, "%s/.license", instance_path);
            if (licpath && am_file_exists(licpath) == 0) {
                am_free(licpath);
                fprintf(stderr, "\nYou have to accept ForgeRock Web Policy Agent license terms to continue.\n"
                        "Please run agentadmin with -x option or interactively to view and accept the license.\n\n");
                net_shutdown();
                return (EXIT_FAILURE);
            }
            am_free(licpath);
            licpath = NULL;
        }

        if (cnt == 1) {
            url_t u, ua;
            am_trim(openam_url);
            u = URL(openam_url);
            if (u.error == 0) {
                if (validate_am_host(&u) != 0) {
                    fprintf(stderr, "Error validating OpenAM URL\n");
                    net_shutdown();
                    return (EXIT_FAILURE);
                }
            } else {
                fprintf(stderr, "Invalid OpenAM URL value\n");
                net_shutdown();
                return (EXIT_FAILURE);
            }
            am_trim(agent_url);
            ua = URL(agent_url);
            if (ua.error != 0) {
                fprintf(stderr, "Invalid Agent URL value\n");
                net_shutdown();
                return (EXIT_FAILURE);
            } else {
                am_trim(agent_id);
                am_trim(agent_pass_file);
                if (am_read_password(agent_pass_file, agent_pass) == 0) {
                    fprintf(stderr, "Error reading password file\n");
                    net_shutdown();
                    return (EXIT_FAILURE);
                }
                if (validate_agent(&u, agent_id, agent_pass) != 0) {
                    fprintf(stderr, "%s\n", PROFILE_NOT_FOUND);
                    net_shutdown();
                    return (EXIT_FAILURE);
                }
            }
            am_trim(web_conf_path);
            if (web_conf_path == NULL || web_conf_path[0] == '\0') {
                fprintf(stderr, "Varnish vmod directory must not be empty\n");
                net_shutdown();
                return (EXIT_FAILURE);
            } else {
                char *t = NULL;
                asprintf(&t, "%s/libvmod_am.so", web_conf_path);
                if (am_whitespace(web_conf_path) > 0) {
                    fprintf(stderr, "Path to Varnish modules directory must not contain spaces\n");
                    am_free(t);
                    net_shutdown();
                    return (EXIT_FAILURE);
                } else if (am_file_writeable(web_conf_path) == 0) {
                    fprintf(stderr, "Error opening Varnish modules directory\n");
                    am_free(t);
                    net_shutdown();
                    return (EXIT_FAILURE);
                } else if (am_file_exists(t) == 1) {
                    fprintf(stderr, "This Varnish instance is already configured\n");
                    am_free(t);
                    net_shutdown();
                    return (EXIT_FAILURE);
                }
                am_free(t);
            }
            create_varnish_instance(ua);
            fprintf(stderr, "\nVarnish and agent configuration files are here:\n%s\nCheck installation log %s for any errors.\n\n", web_conf_path_out, log_path);
        } else {
            display_usage();
        }
        net_shutdown();
        return (EXIT_SUCCESS);
    }

    print_version(NULL, &version);
    if (version != NULL) {
        snprintf(vers, sizeof (vers), "Version: %s", version);
        free(version);
    }

    initscr();
    cbreak();
    noecho();
    keypad(stdscr, TRUE);
    start_color();
    init_pair(1, COLOR_RED, COLOR_BLACK);
    getmaxyx(stdscr, y, x);

    asprintf(&licpath, "%s/.license", instance_path);
    if (am_file_exists(licpath) == 0) {
        lic = license(y, x, &mr);
        wrefresh(lic);
        delwin(lic);
        lic = NULL;
        if (mr == KEY_F(4)) {
            goto all_done;
        } else {
            am_setup_conf(licpath, NULL);
        }
    }

ret_to_menu:

    refresh();

    mr = mainmenu(y, x);

    if (mr == 0) {
        int cr = configure_step(y, x);
        if (cr == KEY_F(4)) {
            goto ret_to_menu;
        }
    } else if (mr >= 2 && mr < menu_size - 1) {
        memset(remname, 0, sizeof (remname));
        if (sscanf(menu_values[mr], "Remove '%[^''']' instance", remname) == 1) {
            if (remove_step(remname, y, x) == KEY_F(4))
                goto ret_to_menu;
        }
        goto ret_to_menu;
    } else if (mr == 1) {
        encrypt_step(y, x);
        goto ret_to_menu;
    }

all_done:

    am_free(licpath);
    clean_main_menu();
    move(0, 0);
    clrtoeol();
    refresh();
    endwin();
    net_shutdown();
    return (EXIT_SUCCESS);
}

int mainmenu(int height, int width) {
    int ret;
    ITEM **menu_items;
    MENU *main_menu;
    int n_choices, i;

    clean_main_menu();
    build_main_menu(instance_path);

    n_choices = menu_size;
    menu_items = (ITEM **) calloc(n_choices + 1, sizeof (ITEM *));
    for (i = 0; i < n_choices; ++i) {
        menu_items[i] = new_item(menu_values[i], menu_values[i]);
        set_item_userptr(menu_items[i], (void *) menu_selected);
    }
    menu_items[n_choices] = (ITEM *) NULL;
    main_menu = new_menu((ITEM **) menu_items);
    menu_opts_off(main_menu, O_SHOWDESC);

    set_menu_sub(main_menu, derwin(stdscr, 10, 50, 6, 10));

    post_menu(main_menu);
    attron(A_BOLD);
    mvprintw(0, 0, name);
    mvprintw(0, width - strlen(vers), vers);
    attroff(A_BOLD);
    refresh();
    pos_menu_cursor(main_menu);
    while ((i = getch()) != KEY_F(4)) {
        switch (i) {
            case KEY_DOWN:
                menu_driver(main_menu, REQ_DOWN_ITEM);
                break;
            case KEY_UP:
                menu_driver(main_menu, REQ_UP_ITEM);
                break;
            case 10:
            {
                ITEM *cur;
                int (*p)(char *);
                cur = current_item(main_menu);
                p = (int (*)(char *))item_userptr(cur);
                ret = p((char *) item_name(cur));
                pos_menu_cursor(main_menu);
                goto menu_sel;
            }
        }
    }
    if (i == KEY_F(4)) {
        ret = menu_size - 1;
    }

menu_sel:
    unpost_menu(main_menu);
    free_menu(main_menu);
    for (i = 0; i < n_choices; ++i)
        free_item(menu_items[i]);
    return ret;
}

int configure_step(int height, int width) {
    FIELD * field[6];
    FORM *form;
    int ch, page = 0;
    int error = 0;
    char emsg[64];
    url_t u, ua;
    field[0] = new_field(1, width - 20, 8, 10, 0, 0);
    field[1] = new_field(1, width - 20, 8, 10, 0, 0);
    field[2] = new_field(1, (width - 20) / 2, 8, 22, 0, 0);
    field[3] = new_field(1, (width - 20) / 2, 10, 22, 0, 0);
    field[4] = new_field(1, width - 20, 8, 10, 0, 0);
    field[5] = NULL;
    set_field_back(field[0], A_UNDERLINE);
    field_opts_off(field[0], O_AUTOSKIP);
    set_field_back(field[1], A_UNDERLINE);
    field_opts_off(field[1], O_AUTOSKIP);
    set_field_back(field[2], A_UNDERLINE);
    field_opts_off(field[2], O_AUTOSKIP);
    set_field_back(field[3], A_UNDERLINE);
    field_opts_off(field[3], O_AUTOSKIP);
    field_opts_off(field[3], O_PUBLIC); //agent password
    field_opts_off(field[4], O_AUTOSKIP);
    set_field_back(field[4], A_UNDERLINE);
    set_new_page(field[1], TRUE);
    set_new_page(field[2], TRUE);
    set_new_page(field[4], TRUE);
    form = new_form(field);
    clear();
    post_form(form);
    refresh();
    step(0, height, width);
    refresh();
    while ((ch = getch()) != KEY_F(4)) {
menu_continue:
        switch (ch) {
            case KEY_F(2):
                page--;
                if (page < 0) {
                    page = 0;
                } else {
                    form_driver(form, REQ_PREV_PAGE);
                    step(page, height, width);
                }
                break;
            case KEY_F(3):
                //validation
                error = 0;
                emsg[0] = '\0';
                form_driver(form, REQ_VALIDATION);
                switch (page) {
                    case 0://openam url validate
                    {
                        strcpy(openam_url, field_buffer(field[0], 0));
                        am_trim(openam_url);
                        u = URL(openam_url);
                        if (u.error == 0) {
                            if (validate_am_host(&u) != 0) {
                                error = 1;
                                sprintf(emsg, "Error validating OpenAM URL");
                            }
                        } else {
                            error = 1;
                            sprintf(emsg, "Invalid URL value");
                        }
                    }
                        break;
                    case 1://agent url
                        strcpy(agent_url, field_buffer(field[1], 0));
                        am_trim(agent_url);
                        ua = URL(agent_url);
                        if (ua.error != 0) {
                            error = 1;
                            sprintf(emsg, "Invalid URL value");
                        }
                        break;
                    case 2://agent user/pass
                        snprintf(agent_id, sizeof (agent_id), "%s", field_buffer(field[2], 0));
                        snprintf(agent_pass, sizeof (agent_pass), "%s", field_buffer(field[3], 0));
                        am_trim(agent_id);
                        am_trim(agent_pass);
                        if (agent_id == NULL || agent_id[0] == '\0'
                                || agent_pass == NULL || agent_pass[0] == '\0') {
                            error = 1;
                            sprintf(emsg, "Values must not be empty");
                        } else {
                            if (u.error == 0) {
                                if (validate_agent(&u, agent_id, agent_pass) != 0) {
                                    error = 1;
                                    sprintf(emsg, PROFILE_NOT_FOUND);
                                }
                            } else {
                                error = 1;
                                sprintf(emsg, "Invalid OpenAM URL value");
                            }
                        }
                        break;
                    case 3://web container path
                    {
                        char *t = NULL;
                        strcpy(web_conf_path, field_buffer(field[4], 0));
                        am_trim(web_conf_path);
                        if (web_conf_path == NULL || web_conf_path[0] == '\0') {
                            error = 1;
                            sprintf(emsg, "Value must not be empty");
                        } else {
                            asprintf(&t, "%s/libvmod_am.so", web_conf_path);
                            if (am_whitespace(web_conf_path) > 0) {
                                LOG("configure_step() error - path to Varnish modules directory must not contain spaces %s", web_conf_path);
                                error = 1;
                                sprintf(emsg, "Error reading Varnish modules directory");
                            } else if (am_file_writeable(web_conf_path) == 0) {
                                LOG("configure_step() error - vmod directory is not accessible %s", web_conf_path);
                                error = 1;
                                sprintf(emsg, "Error opening Varnish modules directory");
                            } else if (am_file_exists(t) == 1) {
                                LOG("configure_step() vmod directory already contains agent library %s", t);
                                error = 1;
                                sprintf(emsg, "This Varnish instance is already configured");
                            }
                            am_free(t);
                        }
                    }
                        break;
                }
                if (error == 0) {
                    move(13, 10);
                    clrtoeol();
                    page++;
                    if (page > 3) {
                        goto cfg_done;
                    }
                    form_driver(form, REQ_NEXT_PAGE);
                    step(page, height, width);
                } else {
                    move(13, 10);
                    clrtoeol();
                    attron(A_BOLD);
                    attron(COLOR_PAIR(1));
                    mvprintw(13, 10, emsg);
                    attroff(COLOR_PAIR(1));
                    attroff(A_BOLD);
                    form_driver(form, REQ_END_LINE);
                }
                break;
            case 0x08:
            case 0x7f:
            case KEY_DC:
            case KEY_BACKSPACE:
                form_driver(form, REQ_DEL_PREV);
                form_driver(form, REQ_CLR_EOF);
                break;
            case KEY_DOWN:
            case 10://ENTER
            case 9://TAB
                if ((ch == 9 || ch == 10) && (page == 0 || page == 1 || page == 3)) {
                    /*allow enter to accept-validate single-line form fields*/
                    ch = KEY_F(3);
                    refresh();
                    goto menu_continue;
                }
                form_driver(form, REQ_NEXT_FIELD);
                form_driver(form, REQ_END_LINE);
                break;
            case KEY_UP:
                form_driver(form, REQ_PREV_FIELD);
                form_driver(form, REQ_END_LINE);
                break;
            default:
                form_driver(form, ch);
                break;
        }
        refresh();
    }
cfg_done:
    unpost_form(form);
    free_form(form);
    free_field(field[0]);
    free_field(field[1]);
    free_field(field[2]);
    free_field(field[3]);
    clear();
    refresh();
    if (page > 3) {
        curs_set(0);
        create_varnish_instance(ua);
        step(-1, height, width);
        while ((ch = getch()) != KEY_F(4)) {
        }
        curs_set(1);
        clear();
    }
    return ch;
}

void encrypt_step(int height, int width) {
    char encryptpasswd[1024] = "";
    char origpasswd[1024] = "";
    char *keystr;
    char bkeystr[1024] = "";
    FIELD * field[2];
    FORM *form;
    int ch;
    field[0] = new_field(1, (width - 20) / 2, 6, 10 + 22, 0, 0);
    field[1] = NULL;
    set_field_back(field[0], A_UNDERLINE);
    field_opts_off(field[0], O_AUTOSKIP);
    form = new_form(field);
    clear();
    post_form(form);
    refresh();
    attron(A_BOLD);
    mvprintw(0, 0, name);
    mvprintw(0, width - strlen(vers), vers);
    mvprintw(height - 1, 0, cryptm);
    mvprintw(height - 1, width - strlen(quit) - 1, quit);
    mvprintw(6, 16, "Enter password:");
    attroff(A_BOLD);
    move(6, 10 + 22);
    refresh();
    while ((ch = getch()) != KEY_F(4)) {
        switch (ch) {
            case KEY_F(2):
            case 10://ENTER
            {
                form_driver(form, REQ_VALIDATION);
                strcpy(origpasswd, field_buffer(field[0], 0));
                am_trim(origpasswd);
                keystr = am_random_key();
                memset(bkeystr, 0, 1024);
                am_b64encode(keystr, bkeystr);
                encrypt_base64(origpasswd, encryptpasswd, bkeystr);
                move(8, 12);
                clrtoeol();
                move(10, 12);
                clrtoeol();
                /*attron(A_STANDOUT);*/
                mvprintw(8, 12, "Encrypted password: %s", encryptpasswd);
                mvprintw(10, 17, "Encrypion key: %s", bkeystr);
                /*attroff(A_STANDOUT);*/
                move(6, 10 + 22);
                form_driver(form, REQ_END_LINE);
            }
                break;
            case 0x08:
            case 0x7f:
            case KEY_DC:
            case KEY_BACKSPACE:
                form_driver(form, REQ_DEL_PREV);
                form_driver(form, REQ_CLR_EOF);
                break;
            default:
                form_driver(form, ch);
                break;
        }
        refresh();
    }
    unpost_form(form);
    free_field(field[0]);
    free_form(form);
    clear();
    refresh();
}

int remove_instance(const char *agent_instance_name) {
    int n, ret = 0;
    am_conf_p inst = NULL, temp;
    char *p1 = NULL, *p2 = NULL, *p3 = NULL, *p4 = NULL, *p5 = NULL;
    if ((n = am_read_instances(instance_path, &inst)) > 0) {
        if ((temp = am_search_conf(inst, agent_instance_name)) != NULL) {
            LOG("remove_instance() removing %s instance", agent_instance_name);
            asprintf(&p1, "%s/%s", instance_path, agent_instance_name);
            asprintf(&p2, "%s/.agents", instance_path);
            asprintf(&p3, "%s ", agent_instance_name);
            asprintf(&p4, "%s/libvmod_am.so", temp->webpath);
            asprintf(&p5, "%s/vmod_am_lib", temp->webpath);
            LOG("remove_step() removing agent libs from %s", temp->webpath);
            if (unlink(p4) != 0) {
                LOG("remove_step() unlink failed %s (%d)", p4, errno);
            }
            if (unlink(p5) != 0) {
                LOG("remove_step() unlink failed %s (%d)", p5, errno);
            }
            LOG("remove_step() removing agent instance %s", p1);
            am_delete_directory(p1);

            LOG("remove_step() cleaning up configuration %s", p3);
            am_cleanup_conf(p2, p3);
            am_vfree(5, p1, p2, p3, p4, p5);
            ret = 1;
        } else {
            ret = -1;
        }
        am_free_conf(inst);
    }
    return ret;
}

int remove_step(const char *anme, int height, int width) {
    int ch;
    curs_set(0);
    attron(A_BOLD);
    mvprintw(0, 0, name);
    mvprintw(0, width - strlen(vers), vers);
    mvprintw(height - 1, width - strlen(quit) - 1, quit);
    mvprintw(6, 10, "Removing '%s' instance...", anme);
    attroff(A_BOLD);
    if (remove_instance(anme)) {
        move(8, 10);
        clrtoeol();
        mvprintw(8, 10, "Cleaning up configuration... Done.");
        move(6, 10);
        clrtoeol();
        attron(A_BOLD);
        mvprintw(6, 10, "Removing '%s' instance... Done.", anme);
        attroff(A_BOLD);
    }
    ch = getch();
    curs_set(1);
    return ch;
}

void step(int step, int height, int width) {
    attron(A_BOLD);
    mvprintw(0, 0, name);
    mvprintw(0, width - strlen(vers), vers);
    if (step > -1) {
        mvprintw(height - 1, 0, prev);
        if (step == 3)
            mvprintw(height - 1, strlen(prev) + 5, configure);
        else
            mvprintw(height - 1, strlen(prev) + 5, next);
    }
    mvprintw(height - 1, width - strlen(quit) - 1, quit);
    switch (step) {
        case -1:
            mvprintw(6, 10, "Agent configuration results:");
            attroff(A_BOLD);
            mvprintw(8, 10, "OpenAM URL: %s", openam_url);
            mvprintw(9, 11, "Agent URL: %s", agent_url);
            mvprintw(10, 13, "Profile: %s", agent_id);
            mvprintw(12, 10, "Web Server configuration:");
            mvprintw(13, 10, web_conf_path_out);
            LOG("Agent configured:\n OpenAM: %s\n Agent Profile: %s\n Agent URL: %s\n Web Server configuration: %s\n",
                    openam_url, agent_id, agent_url, web_conf_path_out);
            break;
        case 0:
            mvprintw(6, 10, "Enter URL where the OpenAM server is running:");
            attroff(A_BOLD);
            mvprintw(10, 10, "Please include the deployment URI also as shown below:");
            mvprintw(11, 10, "http://openam.sample.com:8080/openam");
            move(8, 10);
            break;
        case 1:
            mvprintw(6, 10, "Enter URL where Agent is protecting the Web Container:");
            attroff(A_BOLD);
            mvprintw(10, 10, "Enter the Agent URL as shown below:");
            mvprintw(11, 10, "http://agent.sample.com:1234");
            move(8, 10);
            break;
        case 2:
            mvprintw(6, 10, "Enter the Agent profile attributes in the OpenAM server:");
            attroff(A_BOLD);
            mvprintw(8, 10, "Profile ID:");
            mvprintw(10, 12, "Password:");
            move(8, 22);
            break;
        case 3:
            mvprintw(6, 10, "Enter the path to Varnish modules directory:");
            attroff(A_BOLD);
            mvprintw(10, 10, "Enter the path to Varnish vmods directory as shown below:");
            mvprintw(11, 10, "/opt/varnish/lib/varnish/vmods");
            move(8, 10);
            break;
    }
}

int create_varnish_instance(url_t ua) {
    int ret = 0;
    char *created_name_path = NULL, *created_name_simple = NULL, *agent_conf_entry = NULL;
    if (am_create_agent_dir(instance_path, &created_name_path, &created_name_simple) == -1) {
        LOG("configure_step() error creating agent instance directory");
    } else {
        LOG("configure_step() agent instance directory %s", created_name_path);
    }
    create_varnish_pa_conf(created_name_path, ua, created_name_simple);
    asprintf(&agent_conf_entry, "%s %s %s", created_name_simple, created_name_path, web_conf_path);
    if (am_setup_conf(int_conf_path, agent_conf_entry) == -1) {
        LOG("configure_step() error registering agent instance, conf path: %s, value: %s, wvalue: %s",
                int_conf_path, agent_conf_entry, web_conf_path);
    } else {
        LOG("configure_step() agent instance registered");
    }
    if (create_varnish_links(web_conf_path, created_name_path) == -1) {
        LOG("configure_step() error installing agent libraries");
    } else {
        LOG("configure_step() agent libraries installed");
    }
    am_vfree(3, agent_conf_entry, created_name_path, created_name_simple);
    return ret;
}

int create_varnish_links(const char *vmp, const char *instp) {
    int ret = 0;
    char *a = NULL, *b = NULL, *c = NULL, *d = NULL;
    asprintf(&a, "%s/../../lib", instp);
    asprintf(&b, "%s/vmod_am_lib", vmp);
    asprintf(&c, "%s/libvmod_am.so", vmp);
    asprintf(&d, "%s/../../lib/libvmod_am.so", instp);
    ret = am_file_link(d, c);
    if (ret == -1)
        LOG("Error creating link from %s to %s (%d)", d, c, errno);
    ret = am_file_link(a, b);
    if (ret == -1)
        LOG("Error creating link from %s to %s (%d)", a, b, errno);
    am_vfree(4, a, b, c, d);
    return ret;
}

void create_varnish_pa_conf(const char *ainstp, url_t url, const char *cafn) {
    char line[BUFSZ];
    char template[BUFSZ];
    FILE *in = NULL, *out = NULL;
    char encryptpasswd[1024] = "";
    char *keystr;
    char bkeystr[1024] = "";
    char *a = NULL, *b = NULL, *e = NULL, *f = NULL;
    asprintf(&a, "%s/config/OpenAMAgentBootstrap.properties", ainstp);
    asprintf(&b, "%s/config/OpenAMAgentConfiguration.properties", ainstp);
    asprintf(&e, "%s/config/am.vcl", ainstp);
    sprintf(web_conf_path_out, e);

    keystr = am_random_key();
    am_b64encode(keystr, bkeystr);
    encrypt_base64(agent_pass, encryptpasswd, bkeystr);

    asprintf(&f, VARNISH_VCL, a, b, (url.ssl == 1 ? "https" : "http"));

    snprintf(template, sizeof (template), "%s/../conf/OpenSSOAgentBootstrap.template", instance_path);
    in = fopen(template, "r");
    out = a != NULL ? fopen(a, "w") : NULL;
    if (in != NULL && out != NULL) {
        while (fgets(line, sizeof (line), in)) {
            if (strncmp(line, "com.sun.identity.agents.config.naming.url =", 43) == 0) {
                fprintf(out, "com.sun.identity.agents.config.naming.url = %s/namingservice\n", openam_url);
            } else if (strncmp(line, "com.sun.identity.agents.config.username =", 41) == 0) {
                fprintf(out, "com.sun.identity.agents.config.username = %s\n", agent_id);
            } else if (strncmp(line, "com.sun.identity.agents.config.password =", 41) == 0) {
                fprintf(out, "com.sun.identity.agents.config.password = %s\n", encryptpasswd);
            } else if (strncmp(line, "com.sun.identity.agents.config.key =", 36) == 0) {
                fprintf(out, "com.sun.identity.agents.config.key = %s\n", bkeystr);
            } else if (strncmp(line, "com.sun.identity.agents.config.debug.file =", 43) == 0) {
                fprintf(out, "com.sun.identity.agents.config.debug.file = %s/logs/debug/amAgent\n", ainstp);
            } else if (strncmp(line, "com.sun.identity.agents.config.local.logfile =", 46) == 0) {
                fprintf(out, "com.sun.identity.agents.config.local.logfile = %s/logs/audit/amAgent_%s_%d.log\n", ainstp, url.host, url.port);
            } else if (strncmp(line, "com.sun.identity.agents.config.profilename =", 44) == 0) {
                fprintf(out, "com.sun.identity.agents.config.profilename = %s\n", agent_id);
            } else if (strncmp(line, "com.sun.identity.agents.config.receive.timeout =", 48) == 0) {
                fprintf(out, "com.sun.identity.agents.config.receive.timeout = 4000\n");
            } else if (strncmp(line, "com.sun.identity.agents.config.connect.timeout =", 48) == 0) {
                fprintf(out, "com.sun.identity.agents.config.connect.timeout = 4000\n");
            } else {
                fputs(line, out);
            }
        }
        fflush(out);
    }
    if (in != NULL) fclose(in);
    if (out != NULL) fclose(out);

    snprintf(template, sizeof (template), "%s/../conf/OpenSSOAgentConfiguration.template", instance_path);
    in = fopen(template, "r");
    out = b != NULL ? fopen(b, "w") : NULL;
    if (in != NULL && out != NULL) {
        while (fgets(line, sizeof (line), in)) {
            if (strncmp(line, "com.sun.identity.agents.config.login.url[0] =", 45) == 0) {
                fprintf(out, "com.sun.identity.agents.config.login.url[0] = %s/UI/Login\n", openam_url);
            } else if (strncmp(line, "com.sun.identity.agents.config.remote.logfile =", 47) == 0) {
                fprintf(out, "com.sun.identity.agents.config.remote.logfile = amAgent_%s_%d.log\n", url.host, url.port);
            } else if (strncmp(line, "com.sun.identity.client.notification.url =", 42) == 0) {
                fprintf(out, "com.sun.identity.client.notification.url = %s/UpdateAgentCacheServlet?shortcircuit=false\n", agent_url);
            } else if (strncmp(line, "com.sun.identity.agents.config.agenturi.prefix =", 48) == 0) {
                fprintf(out, "com.sun.identity.agents.config.agenturi.prefix = %s/amagent\n", agent_url);
            } else if (strncmp(line, "com.sun.identity.agents.config.fqdn.default =", 45) == 0) {
                fprintf(out, "com.sun.identity.agents.config.fqdn.default = %s\n", url.host);
            } else if (strncmp(line, "com.sun.identity.agents.config.cdsso.cdcservlet.url[0] =", 56) == 0) {
                fprintf(out, "com.sun.identity.agents.config.cdsso.cdcservlet.url[0] = %s/cdcservlet\n", openam_url);
            } else if (strncmp(line, "com.sun.identity.agents.config.logout.url[0] =", 46) == 0) {
                fprintf(out, "com.sun.identity.agents.config.logout.url[0] = %s/UI/Logout\n", openam_url);
            } else if (strncmp(line, "com.sun.identity.agents.config.notification.enable =", 52) == 0) {
                fprintf(out, "com.sun.identity.agents.config.notification.enable = true\n");
            } else if (strncmp(line, "com.sun.identity.agents.config.debug.file.rotate =", 50) == 0) {
                fprintf(out, "com.sun.identity.agents.config.debug.file.rotate = true\n");
            } else {
                fputs(line, out);
            }
        }
        fflush(out);
    }
    if (in != NULL) fclose(in);
    if (out != NULL) fclose(out);

    am_setup_conf(e, f); /*save agent vcl template*/

    am_vfree(4, a, b, e, f);
}

void display_usage() {
    fprintf(stderr, "\n"
            "%s usage:\n"
            "\n"
            "agentadmin -l\n"
            "   List agent instances\n\n"
            "agentadmin -x\n"
            "   Accept the license terms\n\n"
            "agentadmin -r agent_1\n"
            "   Remove 'agent_1' instance. Use -l to get a list of all known instances\n\n"
            "agentadmin -e password\n"
            "   Encrypt given password. Outputs base64 encoded password and encryption key\n\n"
            "agentadmin -o openamurl -a agenturl -i agent_profile_id -p path_to_password_file -c "
            "path_to_varnish_vmods\n\n"
            "   Create agent instance with given configuration parameters where:\n"
            "    openamurl is OpenAM server url for example, http://openam.example.com:80/openam\n"
            "    agenturl is Agent server url, http://agent.example.com:80\n"
            "    agent_profile_id is agent profile name in OpenAM\n"
            "    path_to_password_file is a path to agent profile password file\n"
            "    path_to_varnish_vmod is a path to Varnish vmods directory, /opt/varnish/lib/varnish/vmods\n\n"
            "agentadmin -v\n"
            "   View version information\n\n",
            name);
}
