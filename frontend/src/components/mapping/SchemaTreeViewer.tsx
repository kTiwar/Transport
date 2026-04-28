import { useState, useEffect } from 'react';
import type { SchemaNode } from '../../types';
import { ChevronRight, ChevronDown } from 'lucide-react';

const TYPE_COLOR: Record<string, string> = {
  STRING:  'var(--cyan)',
  NUMBER:  'var(--orange)',
  DATE:    'var(--green)',
  BOOLEAN: 'var(--yellow)',
  OBJECT:  'var(--purple)',
  ARRAY:   'var(--pink)',
};

const TYPE_ICON: Record<string, string> = {
  STRING: '🔤', NUMBER: '🔢', DATE: '📅',
  BOOLEAN: '☑', OBJECT: '📂', ARRAY: '📋',
};

// ── Search helpers ────────────────────────────────────────────────────────────

function nodeOrDescendantMatches(node: SchemaNode, term: string): boolean {
  if (!term) return true;
  const t = term.toLowerCase();
  if (node.name.toLowerCase().includes(t) || node.path.toLowerCase().includes(t)) return true;
  return node.children.some((c) => nodeOrDescendantMatches(c, t));
}

function HighlightText({ text, term }: { text: string; term: string }) {
  if (!term) return <>{text}</>;
  const idx = text.toLowerCase().indexOf(term.toLowerCase());
  if (idx === -1) return <>{text}</>;
  return (
    <>
      {text.slice(0, idx)}
      <mark style={{
        background: 'rgba(245,158,11,.28)',
        color: 'var(--yellow)',
        borderRadius: 2,
        padding: '0 1px',
        fontWeight: 700,
      }}>
        {text.slice(idx, idx + term.length)}
      </mark>
      {text.slice(idx + term.length)}
    </>
  );
}

// ── Tree node ─────────────────────────────────────────────────────────────────

function TreeNode({
  node,
  depth = 0,
  onSelect,
  selectedPath,
  searchTerm = '',
}: {
  node: SchemaNode;
  depth?: number;
  onSelect?: (path: string) => void;
  selectedPath?: string | null;
  searchTerm?: string;
}) {
  const hasChildren  = node.children.length > 0;
  const color        = TYPE_COLOR[node.type] ?? 'var(--text2)';
  const icon         = TYPE_ICON[node.type] ?? '•';
  const isSelected   = selectedPath === node.path;
  const [hovered, setHovered] = useState(false);

  // Auto-expand when searching and children match
  const childrenMatch = hasChildren && node.children.some((c) => nodeOrDescendantMatches(c, searchTerm));
  const selfMatches   = !searchTerm || node.name.toLowerCase().includes(searchTerm.toLowerCase())
                        || node.path.toLowerCase().includes(searchTerm.toLowerCase());

  const [expanded, setExpanded] = useState(depth < 2);
  useEffect(() => {
    if (searchTerm && childrenMatch) setExpanded(true);
  }, [searchTerm, childrenMatch]);

  // Hide nodes that don't match during search
  if (searchTerm && !selfMatches && !childrenMatch) return null;

  const handleClick = () => {
    if (hasChildren) setExpanded((v) => !v);
    if (!hasChildren) onSelect?.(node.path);
  };

  return (
    <div>
      <div
        className={`tree-node-row${isSelected ? ' selected' : ''}`}
        onClick={handleClick}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        title={node.path}
        style={{
          paddingLeft:  depth * 16 + 6,
          paddingRight: 8,
          paddingTop:   4,
          paddingBottom: 4,
          cursor: 'pointer',
          background: isSelected
            ? 'rgba(0,212,255,.18)'
            : hovered
              ? 'rgba(0,212,255,.16)'
              : searchTerm && selfMatches
                ? 'rgba(245,158,11,.08)'
                : 'transparent',
          borderColor: isSelected
            ? 'rgba(0,212,255,.6)'
            : hovered
              ? 'rgba(0,212,255,.4)'
              : searchTerm && selfMatches
                ? 'rgba(245,158,11,.3)'
                : 'transparent',
          boxShadow: hovered && !isSelected
            ? 'inset 3px 0 0 rgba(0,212,255,.5)'
            : isSelected
              ? 'inset 3px 0 0 var(--cyan)'
              : 'none',
        }}
      >
        {/* Expand / collapse arrow */}
        <span style={{ width: 14, flexShrink: 0, display: 'inline-flex', alignItems: 'center' }}>
          {hasChildren
            ? expanded
              ? <ChevronDown  size={11} style={{ color: 'var(--muted)' }} />
              : <ChevronRight size={11} style={{ color: 'var(--muted)' }} />
            : null}
        </span>

        {/* Type icon */}
        <span style={{ fontSize: 10, flexShrink: 0, marginRight: 2, opacity: .85 }}>{icon}</span>

        {/* Type badge */}
        <span style={{
          fontSize: 9,
          fontFamily: "'Fira Code', monospace",
          background: `${color}18`,
          color,
          border: `1px solid ${color}40`,
          padding: '1px 4px',
          borderRadius: 3,
          flexShrink: 0,
          minWidth: 42,
          textAlign: 'center',
        }}>
          {node.type}
        </span>

        {/* Field name */}
        <span style={{
          fontFamily: "'Fira Code', monospace",
          fontSize: 12,
          color: isSelected ? 'var(--cyan)' : hovered ? '#ffffff' : hasChildren ? 'var(--text)' : 'var(--text2)',
          fontWeight: hasChildren ? 600 : (isSelected || hovered) ? 600 : 400,
          transition: 'color .1s',
          flex: 1,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}>
          <HighlightText text={node.name} term={searchTerm} />
          {node.isArray && <span style={{ color: 'var(--pink)', marginLeft: 2 }}>[]</span>}
        </span>

        {/* Right side: sample value or selected badge */}
        {isSelected ? (
          <span style={{
            fontSize: 9, color: 'var(--cyan)',
            background: 'rgba(0,212,255,.12)',
            border: '1px solid rgba(0,212,255,.3)',
            padding: '1px 6px', borderRadius: 4, flexShrink: 0, fontFamily: "'Fira Code',monospace",
          }}>
            ✓ selected
          </span>
        ) : node.sampleValue && hovered ? (
          <span style={{
            fontSize: 10, color: 'var(--muted)', fontFamily: "'Fira Code', monospace",
            maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          }}>
            "{node.sampleValue}"
          </span>
        ) : node.isArray && node.arrayCount ? (
          <span style={{
            fontSize: 9, color: 'var(--pink)',
            background: 'rgba(236,72,153,.1)', padding: '1px 5px', borderRadius: 4,
          }}>
            {node.arrayCount}×
          </span>
        ) : null}
      </div>

      {/* Path hint on leaf hover or selected */}
      {!hasChildren && (hovered || isSelected) && (
        <div style={{
          paddingLeft: depth * 16 + 36,
          paddingBottom: 2,
          fontSize: 9,
          color: isSelected ? 'rgba(0,212,255,.7)' : 'var(--muted)',
          fontFamily: "'Fira Code', monospace",
          animation: 'fadeIn .12s ease forwards',
          userSelect: 'text',
        }}>
          {node.path}
        </div>
      )}

      {/* Children */}
      {hasChildren && expanded && (
        <div style={{ borderLeft: `1px solid ${color}20`, marginLeft: depth * 16 + 12 }}>
          {node.children.map((child) => (
            <TreeNode
              key={child.path}
              node={child}
              depth={depth + 1}
              onSelect={onSelect}
              selectedPath={selectedPath}
              searchTerm={searchTerm}
            />
          ))}
        </div>
      )}
    </div>
  );
}

// ── Public component ──────────────────────────────────────────────────────────

export default function SchemaTreeViewer({
  node,
  onSelect,
  selectedPath,
  searchTerm = '',
}: {
  node: SchemaNode;
  onSelect?: (path: string) => void;
  selectedPath?: string | null;
  searchTerm?: string;
}) {
  return (
    <div style={{
      background: 'var(--bg2)',
      border: '1px solid var(--border)',
      borderRadius: 8,
      padding: 6,
      fontFamily: "'Fira Code', monospace",
      height: '100%',
    }}>
      <TreeNode
        node={node}
        depth={0}
        onSelect={onSelect}
        selectedPath={selectedPath}
        searchTerm={searchTerm}
      />
    </div>
  );
}
