/*
 * 日付入力用のカレンダー(喫茶 生成り の画面に合わせた見た目)
 *
 * 使い方:<input type="date" name="from" value="2026-11-01" data-calendar> のように data-calendar を付ける。
 *   - 元の入力欄は送信用に hidden にし、代わりに文字で入力できる欄と、カレンダーを開くボタンを置く
 *   - 文字の入力欄には「2026/11/01」「20261101」「2026-11-1」「11/1」(今年)などで直接入力できる
 *     (全角の数字も可)。入力欄から出たとき・Enter を押したときに「2026/11/01(日)」の形に整える
 *   - 日付として読めないときは赤い枠とメッセージを出し、フォームを送らない
 *   - カレンダーのボタンを押すと開き、日付を押すと決まる
 *   - 同じフォームに data-calendar が 2 つあれば、1 つ目〜2 つ目の期間を色付けする
 *   - 送る値(yyyy-MM-dd)は今までと同じ。JavaScript が動かないときは、ブラウザ標準の日付入力のまま使える
 * キーボード:入力欄で ↓ または Alt+↓ でカレンダーを開く。カレンダーの中は矢印で 1 日/1 週間移動、
 *            PageUp/PageDown で前月/翌月、Home/End で週の始め/終わり、Enter・Space で決定、Esc で閉じる
 */
(function () {
  'use strict';

  var WEEK = ['日', '月', '火', '水', '木', '金', '土'];
  var openPicker = null;   // 今開いているカレンダー(同時に 1 つだけ開く)

  var ICON_CAL = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"'
      + ' stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'
      + '<rect x="4" y="5" width="16" height="15" rx="2"></rect><path d="M4 10h16M9 3v4M15 3v4"></path></svg>';

  function pad(n) { return (n < 10 ? '0' : '') + n; }

  /** Date → "2026-11-01"(送る値) */
  function toValue(d) { return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()); }

  /** 年・月・日 → Date(ありえない日付なら null) */
  function makeDate(y, m, d) {
    if (y < 1000 || y > 9999 || m < 1 || m > 12 || d < 1 || d > 31) { return null; }
    var date = new Date(y, m - 1, d);
    return date.getMonth() === m - 1 ? date : null;
  }

  /** "2026-11-01" → Date(読めないときは null) */
  function parseValue(s) {
    var m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(s || '');
    return m ? makeDate(+m[1], +m[2], +m[3]) : null;
  }

  /**
   * 手で入力した文字 → Date(読めないときは null)
   * 「2026/11/01」「2026-11-1」「2026.11.1」「2026年11月1日」「20261101」「11/1」(今年)、
   * 全角の数字、後ろの「(日)」などの曜日も受け付ける
   */
  function parseText(s) {
    s = (s || '')
        .replace(/[０-９]/g, function (c) { return String.fromCharCode(c.charCodeAt(0) - 0xFEE0); })
        .replace(/[／．－ー―‐]/g, '/')
        .replace(/[(（][^)）]*[)）]/g, '')     // 曜日 (日) を取る
        .replace(/日/g, '')
        .replace(/[年月\-.\s]/g, '/')
        .replace(/\/+/g, '/')
        .replace(/^\/|\/$/g, '');
    var m;
    if ((m = /^(\d{4})(\d{2})(\d{2})$/.exec(s))) { return makeDate(+m[1], +m[2], +m[3]); }
    if ((m = /^(\d{4})\/(\d{1,2})\/(\d{1,2})$/.exec(s))) { return makeDate(+m[1], +m[2], +m[3]); }
    if ((m = /^(\d{1,2})\/(\d{1,2})$/.exec(s))) { return makeDate(today().getFullYear(), +m[1], +m[2]); }
    return null;
  }

  /** Date → "2026/11/01(日)"(画面に出す文字) */
  function toLabel(d) {
    return d.getFullYear() + '/' + pad(d.getMonth() + 1) + '/' + pad(d.getDate()) + '(' + WEEK[d.getDay()] + ')';
  }

  function sameDay(a, b) { return a && b && a.getTime() === b.getTime(); }

  function today() { var t = new Date(); return new Date(t.getFullYear(), t.getMonth(), t.getDate()); }

  function addDays(d, n) { return new Date(d.getFullYear(), d.getMonth(), d.getDate() + n); }

  /** 月を動かす(31 日 → 翌月に 31 日がなければ月末にする) */
  function addMonths(d, n) {
    var y = d.getFullYear(), m = d.getMonth() + n;
    var last = new Date(y, m + 1, 0).getDate();
    return new Date(y, m, Math.min(d.getDate(), last));
  }

  function el(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) { e.className = cls; }
    if (text != null) { e.textContent = text; }
    return e;
  }

  function Picker(input, index, group) {
    this.input = input;
    this.group = group;          // 同じフォームの日付入力(期間の色付けに使う)
    this.id = 'cal-' + (input.id || input.name) + '-' + index;
    this.label = input.getAttribute('aria-label')
        || (input.labels && input.labels[0] ? input.labels[0].textContent.trim() : '日付');

    // 元の入力欄は送信用に残して隠す
    input.type = 'hidden';

    var wrap = el('span', 'cal');
    input.parentNode.insertBefore(wrap, input);
    wrap.appendChild(input);

    // 文字の入力欄 + カレンダーを開くボタン(枠は 1 つにまとめる)
    var field = el('span', 'cal-field');
    var text = el('input', 'cal-input');
    text.type = 'text';
    text.inputMode = 'numeric';
    text.autocomplete = 'off';
    text.placeholder = 'YYYY/MM/DD';
    text.setAttribute('aria-label', this.label + '(例:2026/11/01)');
    text.setAttribute('aria-describedby', this.id + '-error');
    if (input.id) {
      // <label for="from"> で文字の入力欄に入れるように、id を移す
      text.id = input.id;
      input.removeAttribute('id');
    }
    if (input.required) { text.required = true; }

    var toggle = el('button', 'cal-toggle');
    toggle.type = 'button';
    toggle.setAttribute('aria-haspopup', 'dialog');
    toggle.setAttribute('aria-expanded', 'false');
    toggle.setAttribute('aria-controls', this.id);
    toggle.setAttribute('aria-label', this.label + 'をカレンダーから選ぶ');
    toggle.innerHTML = ICON_CAL;

    field.appendChild(text);
    field.appendChild(toggle);
    wrap.appendChild(field);

    var error = el('span', 'cal-error');
    error.id = this.id + '-error';
    error.setAttribute('role', 'alert');
    wrap.appendChild(error);

    var panel = el('div', 'cal-panel');
    panel.id = this.id;
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-label', this.label + 'を選ぶ');
    panel.hidden = true;
    panel.innerHTML =
        '<div class="cal-head">'
        + '<button type="button" class="cal-nav" data-move="-1" aria-label="前の月">'
        + '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"'
        + ' stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 6l-6 6 6 6"></path></svg></button>'
        + '<p class="cal-title" aria-live="polite"></p>'
        + '<button type="button" class="cal-nav" data-move="1" aria-label="次の月">'
        + '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"'
        + ' stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 6l6 6-6 6"></path></svg></button>'
        + '</div>'
        + '<table class="cal-grid" role="grid"><thead><tr></tr></thead><tbody></tbody></table>'
        + '<div class="cal-foot"><button type="button" class="cal-today">今日</button></div>';
    wrap.appendChild(panel);

    var headRow = panel.querySelector('thead tr');
    WEEK.forEach(function (w, i) {
      var th = el('th', i === 0 ? 'is-sun' : (i === 6 ? 'is-sat' : ''), w);
      th.scope = 'col';
      headRow.appendChild(th);
    });

    this.wrap = wrap;
    this.field = field;
    this.text = text;
    this.toggle = toggle;
    this.error = error;
    this.panel = panel;
    this.title = panel.querySelector('.cal-title');
    this.body = panel.querySelector('tbody');
    this.focusDay = null;

    var self = this;
    toggle.addEventListener('click', function () { self.isOpen() ? self.close(true) : self.open(true); });

    // 入力中:読める日付になったら、開いているカレンダーをその月に合わせる
    text.addEventListener('input', function () {
      self.setError('');
      var d = parseText(text.value);
      if (d && self.isOpen()) { self.focusDay = d; self.render(false); }
    });
    // 入力欄から出たとき・Enter:値を決めて表示を整える
    text.addEventListener('change', function () { self.commitText(); });
    text.addEventListener('focus', function () { text.select(); });
    text.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') {
        if (!self.commitText()) { e.preventDefault(); }   // 読めないときは送らない
      } else if (e.key === 'ArrowDown') {
        e.preventDefault();
        self.commitText();
        self.open(true);
      } else if (e.key === 'Escape' && self.isOpen()) {
        e.preventDefault();
        self.close(false);
      }
    });

    panel.querySelectorAll('.cal-nav').forEach(function (b) {
      b.addEventListener('click', function () {
        self.focusDay = addMonths(self.focusDay, +b.getAttribute('data-move'));
        self.render(false);
      });
    });
    panel.querySelector('.cal-today').addEventListener('click', function () { self.choose(today()); });
    this.body.addEventListener('click', function (e) {
      var b = e.target.closest('button[data-date]');
      if (b) { self.choose(parseValue(b.getAttribute('data-date'))); }
    });
    panel.addEventListener('keydown', function (e) { self.onKey(e); });

    // 送信前に、入力中の文字を確かめる
    if (input.form) {
      input.form.addEventListener('submit', function (e) {
        if (!self.commitText()) {
          e.preventDefault();
          text.focus();
        }
      });
    }

    this.showValue();
  }

  Picker.prototype.value = function () { return parseValue(this.input.value); };

  Picker.prototype.isOpen = function () { return !this.panel.hidden; };

  /** 決まっている値を「2026/11/01(日)」の形で入力欄に出す */
  Picker.prototype.showValue = function () {
    var v = this.value();
    this.text.value = v ? toLabel(v) : '';
  };

  Picker.prototype.setError = function (message) {
    this.error.textContent = message;
    this.field.classList.toggle('is-invalid', !!message);
    if (message) {
      this.text.setAttribute('aria-invalid', 'true');
    } else {
      this.text.removeAttribute('aria-invalid');
    }
  };

  /**
   * 入力欄の文字を読んで値を決める
   * @return 正しく決まった(または空で、必須でない)とき true
   */
  Picker.prototype.commitText = function () {
    var s = this.text.value.trim();
    if (s === '') {
      this.input.value = '';
      if (this.text.required) {
        this.setError('日付を入力してください');
        return false;
      }
      this.setError('');
      return true;
    }
    var d = parseText(s);
    if (!d) {
      this.setError('日付は 2026/11/01 のように入力してください');
      return false;
    }
    this.input.value = toValue(d);
    this.setError('');
    this.showValue();
    this.refreshGroup();
    return true;
  };

  /** 期間のもう片方が開いていれば、帯の色付けを更新する */
  Picker.prototype.refreshGroup = function () {
    this.group.forEach(function (p) { if (p.isOpen()) { p.render(false); } });
  };

  Picker.prototype.open = function (moveFocus) {
    if (openPicker && openPicker !== this) { openPicker.close(false); }
    openPicker = this;
    this.focusDay = parseText(this.text.value) || this.value() || today();
    this.panel.classList.remove('is-right');
    this.panel.hidden = false;
    this.toggle.setAttribute('aria-expanded', 'true');
    // 画面の右端からはみ出すときは、入力欄の右端にそろえる
    if (this.panel.getBoundingClientRect().right > document.documentElement.clientWidth - 8) {
      this.panel.classList.add('is-right');
    }
    this.render(moveFocus);
  };

  Picker.prototype.close = function (returnFocus) {
    this.panel.hidden = true;
    this.toggle.setAttribute('aria-expanded', 'false');
    if (openPicker === this) { openPicker = null; }
    if (returnFocus) { this.text.focus(); }
  };

  Picker.prototype.choose = function (d) {
    if (!d) { return; }
    this.input.value = toValue(d);
    this.setError('');
    this.showValue();
    this.close(true);
    this.refreshGroup();
  };

  /** 期間(1 つ目〜2 つ目)。そろっていなければ null */
  Picker.prototype.range = function () {
    if (this.group.length !== 2) { return null; }
    var a = this.group[0].value(), b = this.group[1].value();
    return a && b && a <= b ? [a, b] : null;
  };

  Picker.prototype.render = function (moveFocus) {
    var f = this.focusDay, y = f.getFullYear(), m = f.getMonth();
    var selected = this.value(), now = today(), range = this.range();
    this.title.textContent = y + '年' + (m + 1) + '月';

    var first = new Date(y, m, 1);
    var start = addDays(first, -first.getDay());   // 表の左上(日曜日)
    var weeks = Math.ceil((first.getDay() + new Date(y, m + 1, 0).getDate()) / 7);
    this.body.textContent = '';

    for (var w = 0; w < weeks; w++) {
      var tr = el('tr');
      for (var i = 0; i < 7; i++) {
        var d = addDays(start, w * 7 + i);
        var td = el('td');
        td.setAttribute('role', 'gridcell');
        if (d.getMonth() !== m) {
          tr.appendChild(td);   // ほかの月の日は空欄
          continue;
        }
        var b = el('button', 'cal-day', String(d.getDate()));
        b.type = 'button';
        b.setAttribute('data-date', toValue(d));
        b.setAttribute('aria-label', toLabel(d));
        b.tabIndex = sameDay(d, f) ? 0 : -1;
        if (i === 0) { b.classList.add('is-sun'); }
        if (i === 6) { b.classList.add('is-sat'); }
        if (sameDay(d, now)) { b.classList.add('is-today'); b.setAttribute('aria-current', 'date'); }
        if (sameDay(d, selected)) { b.classList.add('is-selected'); b.setAttribute('aria-pressed', 'true'); }
        if (range && d >= range[0] && d <= range[1]) {
          td.classList.add('in-range');
          if (sameDay(d, range[0])) { td.classList.add('range-start'); }
          if (sameDay(d, range[1])) { td.classList.add('range-end'); }
        }
        td.appendChild(b);
        tr.appendChild(td);
      }
      this.body.appendChild(tr);
    }
    if (moveFocus) {
      var cur = this.body.querySelector('button[tabindex="0"]');
      if (cur) { cur.focus(); }
    }
  };

  Picker.prototype.onKey = function (e) {
    if (e.key === 'Escape') { e.preventDefault(); this.close(true); return; }
    if (!e.target.classList.contains('cal-day')) { return; }
    var f = this.focusDay, next = null;
    switch (e.key) {
      case 'ArrowLeft':  next = addDays(f, -1); break;
      case 'ArrowRight': next = addDays(f, 1); break;
      case 'ArrowUp':    next = addDays(f, -7); break;
      case 'ArrowDown':  next = addDays(f, 7); break;
      case 'PageUp':     next = addMonths(f, -1); break;
      case 'PageDown':   next = addMonths(f, 1); break;
      case 'Home':       next = addDays(f, -f.getDay()); break;
      case 'End':        next = addDays(f, 6 - f.getDay()); break;
      default: return;
    }
    e.preventDefault();
    this.focusDay = next;
    this.render(true);
  };

  // カレンダーの外を押したら閉じる
  document.addEventListener('mousedown', function (e) {
    if (openPicker && !openPicker.wrap.contains(e.target)) { openPicker.close(false); }
  });

  document.addEventListener('DOMContentLoaded', function () {
    var groups = new Map();
    document.querySelectorAll('input[type="date"][data-calendar]').forEach(function (input) {
      var key = input.form || document;
      if (!groups.has(key)) { groups.set(key, []); }
      groups.get(key).push(input);
    });
    groups.forEach(function (inputs) {
      var group = [];
      inputs.forEach(function (input, i) { group.push(new Picker(input, i, group)); });
    });
  });
})();
