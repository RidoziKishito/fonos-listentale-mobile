import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { ArrowLeft, Heart, Share2, Star, Play, BookOpen, Download, Headphones, Clock, Info, SkipBack, SkipForward, Maximize2, MessageSquare, Send } from 'lucide-react';

// ---- BOOK DETAIL ----
export function BookDetail() {
  const navigate = useNavigate();
  const { id } = useParams();

  return (
    <div className="flex flex-col h-full bg-white relative">
      {/* Header */}
      <div className="absolute top-0 left-0 right-0 p-6 flex justify-between items-center z-10 pt-safe">
        <button onClick={() => navigate(-1)} className="w-10 h-10 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center text-white">
          <ArrowLeft size={20} />
        </button>
        <div className="flex gap-3">
          <button className="w-10 h-10 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center text-white">
            <Heart size={20} />
          </button>
          <button className="w-10 h-10 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center text-white">
            <Share2 size={20} />
          </button>
        </div>
      </div>

      {/* Cover Background */}
      <div className="h-[45%] relative">
        <img src="https://images.unsplash.com/photo-1711185892188-13f35959d3ca?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxmYW50YXN5JTIwYm9vayUyMGNvdmVyfGVufDF8fHx8MTc3ODc2Mjk4MXww&ixlib=rb-4.1.0&q=80&w=1080" alt="Cover" className="w-full h-full object-cover" />
        <div className="absolute inset-0 bg-gradient-to-t from-white via-white/20 to-transparent"></div>
      </div>

      {/* Content */}
      <div className="flex-1 bg-white -mt-10 rounded-t-[40px] relative z-10 px-6 pt-8 pb-24 overflow-y-auto">
        <div className="flex justify-between items-start mb-2">
          <h1 className="text-2xl font-bold text-slate-900 leading-tight flex-1 pr-4">The Lost Realm</h1>
          <div className="flex items-center gap-1 text-amber-500 bg-amber-50 px-2.5 py-1 rounded-full shrink-0">
            <Star size={14} className="fill-amber-500" />
            <span className="text-sm font-bold">4.8</span>
          </div>
        </div>
        <p className="text-slate-500 font-medium mb-6">by Sarah J. Maas</p>

        <div className="flex justify-between border-y border-slate-100 py-4 mb-6">
          <div className="text-center flex-1">
            <div className="flex items-center justify-center gap-1.5 text-slate-700 mb-1">
              <Headphones size={16} />
              <span className="font-semibold text-sm">Audio</span>
            </div>
            <p className="text-xs text-slate-500">12h 30m</p>
          </div>
          <div className="w-px bg-slate-100"></div>
          <div className="text-center flex-1">
            <div className="flex items-center justify-center gap-1.5 text-slate-700 mb-1">
              <BookOpen size={16} />
              <span className="font-semibold text-sm">Ebook</span>
            </div>
            <p className="text-xs text-slate-500">420 pages</p>
          </div>
          <div className="w-px bg-slate-100"></div>
          <div className="text-center flex-1">
            <div className="flex items-center justify-center gap-1.5 text-slate-700 mb-1">
              <Info size={16} />
              <span className="font-semibold text-sm">Genre</span>
            </div>
            <p className="text-xs text-slate-500">Fantasy</p>
          </div>
        </div>

        <div className="mb-6">
          <h2 className="text-lg font-bold text-slate-900 mb-2">Synopsis</h2>
          <p className="text-sm text-slate-600 leading-relaxed">
            In a world where magic has been outlawed, a young woman discovers she possesses a power that could change everything. As dark forces gather...
            <span className="text-violet-600 font-medium ml-1">Read more</span>
          </p>
        </div>

        <div>
          <div className="flex justify-between items-end mb-4">
            <h2 className="text-lg font-bold text-slate-900">Reviews (1.2k)</h2>
            <button onClick={() => navigate(`/review/${id}`)} className="text-sm text-violet-600 font-medium">See all</button>
          </div>
          <div className="bg-slate-50 p-4 rounded-2xl">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 bg-slate-200 rounded-full overflow-hidden">
                  <img src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9maWxlJTIwcGljdHVyZXxlbnwxfHx8fDE3Nzg4MjYwMjZ8MA&ixlib=rb-4.1.0&q=80&w=1080" alt="User" />
                </div>
                <span className="text-sm font-semibold text-slate-900">Jessica T.</span>
              </div>
              <div className="flex text-amber-400"><Star size={12} className="fill-amber-400"/><Star size={12} className="fill-amber-400"/><Star size={12} className="fill-amber-400"/><Star size={12} className="fill-amber-400"/><Star size={12} className="fill-amber-400"/></div>
            </div>
            <p className="text-xs text-slate-600">Absolutely loved the world building in this one! The narration in the audiobook is top tier.</p>
          </div>
          <button onClick={() => navigate(`/review/create/${id}`)} className="w-full mt-3 py-3 border-2 border-dashed border-slate-200 rounded-xl text-slate-500 text-sm font-medium hover:border-violet-300 hover:text-violet-600 transition-colors">
            Write a Review
          </button>
        </div>
      </div>

      {/* Action Bar */}
      <div className="absolute bottom-0 left-0 right-0 p-4 bg-white border-t border-slate-100 flex gap-3 pb-6">
        <button onClick={() => navigate(`/player/${id}`)} className="flex-1 bg-violet-600 text-white font-semibold py-3.5 rounded-2xl flex items-center justify-center gap-2 shadow-sm shadow-violet-600/25 active:scale-95 transition-transform">
          <Play size={20} className="fill-white" />
          Listen
        </button>
        <button onClick={() => navigate(`/reader/${id}`)} className="flex-1 bg-violet-50 text-violet-600 font-semibold py-3.5 rounded-2xl flex items-center justify-center gap-2 active:scale-95 transition-transform">
          <BookOpen size={20} />
          Read
        </button>
      </div>
    </div>
  );
}

// ---- AUDIO PLAYER ----
export function AudioPlayer() {
  const navigate = useNavigate();
  const [isPlaying, setIsPlaying] = useState(false);

  return (
    <div className="flex flex-col h-full bg-slate-900 text-white relative">
      {/* Blurred Background */}
      <div className="absolute inset-0 overflow-hidden opacity-30">
        <img src="https://images.unsplash.com/photo-1711185892188-13f35959d3ca?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxmYW50YXN5JTIwYm9vayUyMGNvdmVyfGVufDF8fHx8MTc3ODc2Mjk4MXww&ixlib=rb-4.1.0&q=80&w=1080" alt="Cover Blur" className="w-full h-full object-cover blur-3xl scale-110" />
      </div>

      <div className="relative z-10 flex flex-col h-full">
        {/* Header */}
        <div className="p-6 flex justify-between items-center">
          <button onClick={() => navigate(-1)} className="p-2 -ml-2 text-white">
            <ArrowLeft size={24} />
          </button>
          <span className="text-sm font-semibold opacity-80">Now Playing</span>
          <button className="p-2 -mr-2 text-white">
            <MoreHorizontalIcon />
          </button>
        </div>

        {/* Artwork */}
        <div className="flex-1 flex items-center justify-center p-8">
          <div className="w-full aspect-square max-w-[280px] rounded-3xl overflow-hidden shadow-2xl shadow-black/50">
            <img src="https://images.unsplash.com/photo-1711185892188-13f35959d3ca?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxmYW50YXN5JTIwYm9vayUyMGNvdmVyfGVufDF8fHx8MTc3ODc2Mjk4MXww&ixlib=rb-4.1.0&q=80&w=1080" alt="Cover" className="w-full h-full object-cover" />
          </div>
        </div>

        {/* Controls Area */}
        <div className="p-8 pb-12 bg-gradient-to-t from-slate-900 via-slate-900 to-transparent">
          <div className="text-center mb-8">
            <h2 className="text-2xl font-bold mb-1">The Lost Realm</h2>
            <p className="text-slate-400 text-sm">Chapter 4: The Awakening</p>
          </div>

          {/* Progress */}
          <div className="mb-8">
            <div className="w-full h-2 bg-white/20 rounded-full mb-3 relative cursor-pointer">
              <div className="absolute top-0 left-0 h-full bg-violet-500 rounded-full" style={{ width: '35%' }}></div>
              <div className="absolute top-1/2 left-[35%] -translate-y-1/2 -translate-x-1/2 w-4 h-4 bg-white rounded-full shadow-md"></div>
            </div>
            <div className="flex justify-between text-xs text-slate-400 font-medium">
              <span>12:40</span>
              <span>45:20</span>
            </div>
          </div>

          {/* Main Controls */}
          <div className="flex items-center justify-center gap-8">
            <button className="text-slate-400 hover:text-white transition-colors">
              <Clock size={24} />
            </button>
            <button className="text-white">
              <SkipBack size={32} />
            </button>
            <button 
              onClick={() => setIsPlaying(!isPlaying)}
              className="w-20 h-20 bg-violet-500 rounded-full flex items-center justify-center text-white shadow-lg shadow-violet-500/30 active:scale-95 transition-transform"
            >
              {isPlaying ? (
                <div className="w-6 h-6 border-l-4 border-r-4 border-white" />
              ) : (
                <Play size={32} className="fill-white ml-2" />
              )}
            </button>
            <button className="text-white">
              <SkipForward size={32} />
            </button>
            <button className="text-slate-400 hover:text-white transition-colors">
              <Download size={24} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ---- EBOOK READER ----
export function EbookReader() {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col h-full bg-[#fdfaf6] text-slate-800">
      {/* Header */}
      <div className="p-4 flex justify-between items-center border-b border-slate-200/50 bg-[#fdfaf6]">
        <button onClick={() => navigate(-1)} className="p-2 -ml-2">
          <ArrowLeft size={20} />
        </button>
        <span className="text-xs font-semibold text-slate-500 tracking-wider uppercase">Chapter 4</span>
        <button className="p-2 -mr-2">
          <Settings size={20} />
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 p-6 overflow-y-auto leading-relaxed text-lg font-serif">
        <h1 className="text-3xl font-bold mb-8 text-slate-900 text-center font-sans">The Awakening</h1>
        <p className="mb-6 indent-8">
          The sun had barely broken the horizon when Elara felt the first tremor. It wasn't the shaking of the earth, but rather a resonance in her very bones. The old tales spoke of this—the moment when the ancient magic, long dormant, would finally stir.
        </p>
        <p className="mb-6 indent-8">
          She pushed aside the heavy woolen blankets and stepped onto the cold stone floor of her chamber. The fire had died down to glowing embers, casting long, dancing shadows against the tapestries that lined the walls.
        </p>
        <p className="mb-6 indent-8">
          "It's starting," she whispered, her voice barely louder than the crackle of the dying fire. She grabbed her cloak and hurried out into the corridor. The castle was still asleep, oblivious to the momentous change that was unfolding beneath their feet.
        </p>
      </div>

      {/* Footer Progress */}
      <div className="p-4 bg-[#fdfaf6] border-t border-slate-200/50 flex items-center gap-4 text-xs font-medium text-slate-400">
        <span>34%</span>
        <div className="flex-1 h-1 bg-slate-200 rounded-full">
          <div className="h-full bg-slate-400 rounded-full" style={{ width: '34%' }}></div>
        </div>
        <span>Page 142 / 420</span>
      </div>
    </div>
  );
}

// ---- REVIEW FLOW ----
export function ReviewCreate() {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="p-4 flex justify-between items-center border-b border-slate-100">
        <button onClick={() => navigate(-1)} className="text-slate-500 font-medium">Cancel</button>
        <h1 className="text-base font-bold text-slate-900">Write Review</h1>
        <button className="text-violet-600 font-bold">Post</button>
      </div>

      <div className="p-6">
        <div className="flex gap-4 items-center mb-8">
          <img src="https://images.unsplash.com/photo-1711185892188-13f35959d3ca?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxmYW50YXN5JTIwYm9vayUyMGNvdmVyfGVufDF8fHx8MTc3ODc2Mjk4MXww&ixlib=rb-4.1.0&q=80&w=1080" alt="Cover" className="w-16 h-24 object-cover rounded-lg shadow-sm" />
          <div>
            <h2 className="font-bold text-slate-900">The Lost Realm</h2>
            <p className="text-sm text-slate-500">Sarah J. Maas</p>
          </div>
        </div>

        <div className="flex justify-center gap-2 mb-8">
          {[1,2,3,4,5].map(i => (
             <button key={i} className="p-1">
               <Star size={36} className="text-slate-200 hover:text-amber-400 hover:fill-amber-400 transition-all" />
             </button>
          ))}
        </div>

        <textarea 
          placeholder="What did you think of the book?" 
          className="w-full h-40 bg-slate-50 rounded-2xl p-4 border border-slate-200 outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500 resize-none text-slate-700"
        ></textarea>
      </div>
    </div>
  );
}

export function ReviewDetail() {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col h-full bg-slate-50">
      <div className="p-4 flex items-center gap-4 bg-white border-b border-slate-100 z-10">
        <button onClick={() => navigate(-1)} className="p-2 -ml-2 text-slate-800">
          <ArrowLeft size={24} />
        </button>
        <h1 className="text-lg font-bold text-slate-900">Reviews</h1>
      </div>

      <div className="flex-1 overflow-y-auto p-6 space-y-4">
        {[1,2,3,4].map(i => (
          <div key={i} className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-slate-200 rounded-full overflow-hidden">
                  <img src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9maWxlJTIwcGljdHVyZXxlbnwxfHx8fDE3Nzg4MjYwMjZ8MA&ixlib=rb-4.1.0&q=80&w=1080" alt="User" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Jessica T.</h3>
                  <p className="text-xs text-slate-500">2 days ago</p>
                </div>
              </div>
              <div className="flex text-amber-400"><Star size={14} className="fill-amber-400"/><Star size={14} className="fill-amber-400"/><Star size={14} className="fill-amber-400"/><Star size={14} className="fill-amber-400"/><Star size={14} className="fill-amber-400"/></div>
            </div>
            <p className="text-sm text-slate-600 leading-relaxed mb-4">
              Absolutely loved the world building in this one! The narration in the audiobook is top tier. I couldn't stop listening once I started.
            </p>
            <div className="flex items-center gap-4 text-xs font-medium text-slate-500">
              <button className="flex items-center gap-1.5 hover:text-violet-600 transition-colors">
                <Heart size={16} /> 124
              </button>
              <button className="flex items-center gap-1.5 hover:text-violet-600 transition-colors">
                <MessageSquare size={16} /> 12
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function MoreHorizontalIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="1"></circle>
      <circle cx="19" cy="12" r="1"></circle>
      <circle cx="5" cy="12" r="1"></circle>
    </svg>
  )
}
