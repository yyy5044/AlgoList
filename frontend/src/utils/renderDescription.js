import { marked } from 'marked'
import katex from 'katex'
import 'katex/dist/katex.min.css'

/**
 * 문제 본문(description)을 화면에 표시할 HTML로 변환한다.
 *
 * 사이트마다 본문 형식이 다르다(BOJ는 MathJax HTML이 섞인 마크다운,
 * 프로그래머스는 순수 HTML 등). 그래서 단일 파이프라인을 거치되,
 * 각 단계는 "해당 패턴이 본문에 있을 때만" 동작하도록 설계했다.
 * 패턴이 없으면 그 단계는 입력을 그대로 통과시키므로(no-op),
 * 어떤 사이트의 본문이 들어와도 안전하게 처리된다.
 *
 * 단계: MathJax 복원 → LaTeX 렌더링 → 마크다운/HTML 변환
 */

// ── 1단계: 백준이 미리 렌더링해 둔 MathJax HTML을 원본 $..$ LaTeX로 되돌린다 ──
// <mjx-container> 안에는 <span class="mjx-copytext">$(x_1, y_1)$</span> 형태로
// 원본 수식이 들어있다. 거대한 MathJax 마크업을 이 원본으로 교체한다.
function restoreMathJaxSource(text) {
  if (!text.includes('mjx-container')) return text
  return text.replace(
    /<mjx-container[^>]*>[\s\S]*?<span[^>]*mjx-copytext[^>]*>([\s\S]*?)<\/span>\s*<\/mjx-container>/g,
    (_, latex) => latex
  )
}

// ── 2단계: $$..$$(블록), $..$(인라인) LaTeX를 KaTeX로 렌더링한다 ──
// marked는 <p> 같은 HTML 블록 안의 $..$를 건드리지 않으므로,
// 마크다운 변환 전에 KaTeX를 직접 호출해 수식부터 HTML로 만든다.
function renderMath(text) {
  if (!text.includes('$')) return text

  // 블록 수식 먼저 처리($$..$$가 $..$에 먼저 잡히지 않도록)
  text = text.replace(/\$\$([\s\S]*?)\$\$/g, (whole, tex) => {
    try {
      return katex.renderToString(tex.trim(), { displayMode: true, throwOnError: false })
    } catch {
      return whole
    }
  })

  // 인라인 수식
  text = text.replace(/\$([^$\n]+?)\$/g, (whole, tex) => {
    try {
      return katex.renderToString(tex.trim(), { displayMode: false, throwOnError: false })
    } catch {
      return whole
    }
  })

  return text
}

// ── 3단계: 마크다운(### 입력 등)과 남은 HTML을 최종 HTML로 변환한다 ──
function renderMarkdown(text) {
  return marked.parse(text)
}

/**
 * @param {string} raw - DB에 저장된 원본 본문
 * @returns {string} 화면에 v-html로 꽂을 HTML
 */
export function renderDescription(raw) {
  if (!raw) return ''
  let html = raw
  html = restoreMathJaxSource(html)
  html = renderMath(html)
  html = renderMarkdown(html)
  return html
}
