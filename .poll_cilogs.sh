#!/usr/bin/env bash
# 轮询 ci-logs 分支，等待本次新构建日志出现（HEAD 变化即新一次构建完成）
set -u
REMOTE="https://ghproxy.net/https://github.com/lindedaoqwq/Minecraft-LinClient.git"
WORK=/workspace
LOG=$WORK/.cilogs.out
: > "$LOG"

git -C "$WORK" fetch -q "$REMOTE" ci-logs 2>/dev/null || true
OLD=$(git -C "$WORK" rev-parse origin/ci-logs 2>/dev/null || echo "")
echo "基线 ci-logs HEAD: ${OLD:-<无>}" | tee -a "$LOG"

for i in $(seq 1 75); do
  git -C "$WORK" fetch -q "$REMOTE" ci-logs 2>/dev/null || true
  NEW=$(git -C "$WORK" rev-parse origin/ci-logs 2>/dev/null || echo "")
  if [ -n "$NEW" ] && [ "$NEW" != "$OLD" ]; then
    echo "检测到新的 ci-logs 提交: $NEW" | tee -a "$LOG"
    echo "提交信息: $(git -C "$WORK" log -1 --format='%ci | %s' "$NEW")" | tee -a "$LOG"
    # 列出该提交下所有 build-*.log 文件并抓取
    mapfile -t FILES < <(git -C "$WORK" ls-tree -r --name-only "$NEW" | grep -E '^build-.*\.log$')
    if [ ${#FILES[@]} -eq 0 ]; then
      echo "该提交下未找到 build-*.log（可能 ci-logs 仅记录了空日志）" | tee -a "$LOG"
    fi
    for f in "${FILES[@]:-}"; do
      [ -z "$f" ] && continue
      git -C "$WORK" show "origin/ci-logs:$f" > "$WORK/$f" 2>/dev/null || { echo "(缺失 $f)" | tee -a "$LOG"; continue; }
      if grep -q "BUILD SUCCESSFUL" "$WORK/$f"; then
        echo "[$f] => BUILD SUCCESSFUL" | tee -a "$LOG"
      elif grep -q "BUILD FAILED" "$WORK/$f"; then
        echo "[$f] => BUILD FAILED" | tee -a "$LOG"
        grep -nE "error:|cannot find symbol|FAILED|What went wrong" "$WORK/$f" | head -40 | tee -a "$LOG"
      else
        echo "[$f] => 无明确结果（日志可能不完整）" | tee -a "$LOG"
      fi
    done
    echo "DONE" | tee -a "$LOG"
    exit 0
  fi
  sleep 15
done
echo "超时未检测到新 ci-logs 提交（构建可能仍在进行，或 ci-logs 推送环节失败）" | tee -a "$LOG"
exit 1
