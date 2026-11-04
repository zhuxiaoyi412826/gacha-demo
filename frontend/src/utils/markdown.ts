import MarkdownIt from 'markdown-it'

// html: false 转义原始 HTML（防 XSS）；breaks: true 让单个换行渲染为 <br>；linkify 自动识别链接
const md = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true
})

/** 将 Markdown 文本渲染为安全 HTML 字符串，配合 v-html + .markdown-body 样式使用 */
export function renderMarkdown(text?: string | null): string {
  if (!text) return ''
  return md.render(normalizeListBreaks(text))
}

/**
 * 条目/句子自动换行：
 * 1. 序号换行：「（1）」「(2)」这类中文/英文括号数字序号（非行首出现时）前补换行，
 *    只匹配数字序号，「（request、response）」这类括号内容不受影响；
 * 2. 句读换行：中文句号「。」、分号「；」后面若还有内容则补换行，让每句独占一行；
 *    句末不追加换行，已有换行时不重复插入。
 * 配合 breaks:true 渲染为 <br>。英文句点「.」不处理（避免误伤版本号、代码等）。
 */
function normalizeListBreaks(text: string): string {
  return text
    .replace(/([。；])(?!\s*$)(?!\n)/g, '$1\n')
    .replace(/(?<!^)(?<!\n)(（\d+）|\(\d+\))/g, '\n$1')
}
