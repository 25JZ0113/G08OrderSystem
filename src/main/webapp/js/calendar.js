/*
 * 日付入力用のカレンダー(喫茶 生成り の画面に合わせた見た目)
 *
 * 使い方:<input type="date" name="from" value="2026-11-01" data-calendar> のように data-calendar を付ける。
 *   - 入力欄は hidden になり、代わりに「2026/11/01(日)」と表示するボタンを置く。送る値(yyyy-MM-dd)はそのまま
 *   - ボタンを押すとカレンダーが開く。日付を押すと決まる
 *   - 同じフォームに data-calendar が 2 つあれば、1 つ目〜2 つ目の期間を色付けする
 *   - JavaScript が動かないときは、ブラウザ標準の日付入力のまま使える
 * キーボード:矢印で 1 日/1 週間移動、PageUp/PageDown で前月/翌月、Home/End で週の始め/終わり、
 *            Enter・Space で決定、Esc で閉じる
 */
(function () {
  'use strict';

  var WEEK = ['日', '月', '火', '水', '木', '金', '土'];
  var openPicker = null;   // 今開いているカレンダー(同時に 1 つだけ開く)

  function pad(n) { return (n < 10 ? '0' : '') + n; }

  /** Date → "2026-11-01"(送る値) */
  function toValue(d) { return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()); }

  /** "2026-11-01" → Date(読めないときは null) */
  function parse(s) {
    var m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(s || '');
    if (!m) { return null; }
    var d = new Date(+m[1], +m[2] - 1, +m[3]);
    return d.getMonth() === +m[2] - 1 ? d : null;
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

    // 入力欄は送信用に残して隠す
    input.type = 'hidden';

    var wrap = el('span', 'cal');
    input.parentNode.insertBefore(wrap, input);
    wrap.appendChild(input);

    var button = el('button', 'cal-field');
    button.type = 'button';
    button.setAttribute('aria-haspopup', 'dialog');
    button.setAttribute('aria-expanded', 'false');
    button.setAttribute('aria-controls', this.id);
    if (input.id) {
      // <label for="from"> を押したときもカレンダーが開くように、id をボタンへ移す
      button.id = input.id;
      input.removeAttribute('id');
    }
    this.text = el('span', 'cal-field-text');
    button.appendChild(this.text);
    button.insertAdjacentHTML('beforeend',
        '<svg class="cal-field-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"'
        + ' stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'
        + '<rect x="4" y="5" width="16" height="15" rx="2"></rect><path d="M4 10h16M9 3v4M15 3v4"></path></svg>');
    wrap.appendChild(button);

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
    this.button = button;
    this.panel = panel;
    this.title = panel.querySelector('.cal-title');
    this.body = panel.querySelector('tbody');
    this.focusDay = null;

    var self = this;
    button.addEventListener('click', function () { self.isOpen() ? self.close(true) : self.open(); });
    panel.querySelectorAll('.cal-nav').forEach(function (b) {
      b.addEventListener('click', function () {
        self.focusDay = addMonths(self.focusDay, +b.getAttribute('data-move'));
        self.render(false);
      });
    });
    panel.querySelector('.cal-today').addEventListener('click', function () { self.choose(today()); });
    this.body.addEventListener('click', function (e) {
      var b = e.target.closest('button[data-date]');
      if (b) { self.choose(parse(b.getAttribute('data-date'))); }
    });
    panel.addEventListener('keydown', function (e) { self.onKey(e); });

    this.updateText();
  }

  Picker.prototype.value = function () { return parse(this.input.value); };

  Picker.prototype.isOpen = function () { return !this.panel.hidden; };

  Picker.prototype.updateText = function () {
    var v = this.value();
    this.text.textContent = v ? toLabel(v) : '日付を選ぶ';
    this.button.setAttribute('aria-label', this.label + ':' + (v ? toLabel(v) : '未選択') + '(押すとカレンダーが開きます)');
  };

  Picker.prototype.open = function () {
    if (openPicker && openPicker !== this) { openPicker.close(false); }
    openPicker = this;
    this.focusDay = this.value() || today();
    this.panel.classList.remove('is-right');
    this.panel.hidden = false;
    this.button.setAttribute('aria-expanded', 'true');
    // 画面の右端からはみ出すときは、ボタンの右端にそろえる
    if (this.panel.getBoundingClientRect().right > document.documentElement.clientWidth - 8) {
      this.panel.classList.add('is-right');
    }
    this.render(true);
  };

  Picker.prototype.close = function (returnFocus) {
    this.panel.hidden = true;
    this.button.setAttribute('aria-expanded', 'false');
    if (openPicker === this) { openPicker = null; }
    if (returnFocus) { this.button.focus(); }
  };

  Picker.prototype.choose = function (d) {
    if (!d) { return; }
    this.input.value = toValue(d);
    this.updateText();
    this.close(true);
    // 期間のもう片方の色付けも更新する
    this.group.forEach(function (p) { if (p.isOpen()) { p.render(false); } });
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
