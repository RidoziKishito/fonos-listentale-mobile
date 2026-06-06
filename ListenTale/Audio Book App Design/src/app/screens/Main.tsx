import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router';
import { Home, Search as SearchIcon, Library as LibraryIcon, Trophy, User, BookOpen, Headphones, Star, Play, Settings, ChevronRight, Bookmark } from 'lucide-react';

export function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { id: '/app', icon: Home, label: 'Books' },
    { id: '/app/challenge', icon: Trophy, label: 'Challenge' },
    { id: '/app/search', icon: SearchIcon, label: 'Search' },
    { id: '/app/library', icon: LibraryIcon, label: 'Library' },
    { id: '/app/profile', icon: User, label: 'Profile' },
  ];

  return (
    <div className="flex flex-col h-full bg-slate-50 relative">
      <div className="flex-1 overflow-y-auto pb-20">
        <Outlet />
      </div>
      
      {/* Bottom Navigation */}
      <div className="absolute bottom-0 left-0 right-0 bg-white border-t border-slate-100 px-6 py-4 flex justify-between items-center pb-6">
        {navItems.map((item) => {
          const isActive = location.pathname === item.id;
          const Icon = item.icon;
          return (
            <button 
              key={item.id} 
              onClick={() => navigate(item.id)}
              className={`flex flex-col items-center gap-1 ${isActive ? 'text-violet-600' : 'text-slate-400'}`}
            >
              <div className={`p-1.5 rounded-xl transition-colors ${isActive ? 'bg-violet-50' : 'bg-transparent'}`}>
                <Icon size={24} strokeWidth={isActive ? 2.5 : 2} />
              </div>
              <span className="text-[10px] font-medium">{item.label}</span>
            </button>
          )
        })}
      </div>
    </div>
  );
}

// ---- BOOKS SCREEN ----
export function Books() {
  const [tab, setTab] = useState('audiobooks');
  const navigate = useNavigate();
  
  return (
    <div className="flex flex-col h-full bg-white">
      <div className="pt-8 px-6 pb-4">
        <h1 className="text-2xl font-bold text-slate-900">Discover</h1>
      </div>
      
      <div className="px-6 mb-6">
        <div className="flex bg-slate-100 p-1 rounded-2xl">
          {['audiobooks', 'ebooks', 'community'].map(t => (
            <button 
              key={t}
              onClick={() => setTab(t)}
              className={`flex-1 py-2 text-sm font-medium rounded-xl capitalize transition-all ${tab === t ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'}`}
            >
              {t === 'community' ? 'Community' : t}
            </button>
          ))}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-6 space-y-8 pb-10">
        {tab === 'audiobooks' && (
          <>
            <div>
              <div className="flex justify-between items-end mb-4">
                <h2 className="text-lg font-bold text-slate-900">Trending Now</h2>
                <button className="text-sm text-violet-600 font-medium">See all</button>
              </div>
              <div className="flex gap-4 overflow-x-auto pb-4 snap-x">
                {[1, 2, 3].map(i => (
                  <div key={i} onClick={() => navigate(`/book/${i}`)} className="min-w-[140px] snap-start cursor-pointer">
                    <img src="https://images.unsplash.com/photo-1711185892188-13f35959d3ca?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxmYW50YXN5JTIwYm9vayUyMGNvdmVyfGVufDF8fHx8MTc3ODc2Mjk4MXww&ixlib=rb-4.1.0&q=80&w=1080" alt="Cover" className="w-full aspect-[2/3] object-cover rounded-2xl shadow-sm mb-3" />
                    <h3 className="font-semibold text-slate-900 line-clamp-1">The Lost Realm</h3>
                    <p className="text-xs text-slate-500">Sarah J. Maas</p>
                  </div>
                ))}
              </div>
            </div>

            <div>
              <h2 className="text-lg font-bold text-slate-900 mb-4">Recommended for You</h2>
              <div className="space-y-4">
                {[1, 2, 3].map(i => (
                  <div key={i} onClick={() => navigate(`/book/${i}`)} className="flex gap-4 items-center cursor-pointer">
                    <img src="https://images.unsplash.com/photo-1571699090656-6ae5d6daabc8?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxhdWRpb2Jvb2slMjBjb3ZlcnxlbnwxfHx8fDE3Nzg4MjU0NTN8MA&ixlib=rb-4.1.0&q=80&w=1080" alt="Cover" className="w-16 h-16 rounded-xl object-cover" />
                    <div className="flex-1">
                      <h3 className="font-semibold text-slate-900">Atomic Habits</h3>
                      <p className="text-xs text-slate-500 mb-1">James Clear</p>
                      <div className="flex items-center gap-2 text-xs text-slate-400">
                        <Star size={12} className="text-amber-400 fill-amber-400" />
                        <span>4.9</span>
                        <span>•</span>
                        <Headphones size={12} />
                        <span>5h 30m</span>
                      </div>
                    </div>
                    <button className="w-8 h-8 rounded-full bg-violet-50 text-violet-600 flex items-center justify-center">
                      <Play size={14} className="ml-0.5" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}

        {tab === 'ebooks' && (
           <div className="flex flex-col items-center justify-center py-20 text-center">
             <BookOpen size={48} className="text-slate-300 mb-4" />
             <h3 className="text-lg font-semibold text-slate-700">Ebooks Library</h3>
             <p className="text-slate-500 text-sm mt-2">Browse thousands of ebooks.</p>
           </div>
        )}

        {tab === 'community' && (
           <div className="flex flex-col items-center justify-center py-20 text-center">
             <Star size={48} className="text-slate-300 mb-4" />
             <h3 className="text-lg font-semibold text-slate-700">Community Reviews</h3>
             <p className="text-slate-500 text-sm mt-2">See what others are reading.</p>
           </div>
        )}
      </div>
    </div>
  );
}

// ---- SEARCH SCREEN ----
export function Search() {
  const [query, setQuery] = useState('');
  
  return (
    <div className="flex flex-col h-full bg-slate-50">
      <div className="pt-8 px-6 pb-4 bg-white shadow-sm z-10">
        <h1 className="text-2xl font-bold text-slate-900 mb-4">Search</h1>
        <div className="relative">
          <SearchIcon className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
          <input 
            type="text" 
            placeholder="Books, authors, or topics" 
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="w-full bg-slate-100 border-none rounded-2xl py-3.5 pl-11 pr-4 outline-none focus:ring-2 focus:ring-violet-500" 
          />
        </div>
      </div>

      <div className="flex-1 px-6 py-6 overflow-y-auto">
        {!query ? (
          <div className="space-y-8">
            <div>
              <h2 className="text-lg font-bold text-slate-900 mb-4">Top Categories</h2>
              <div className="flex flex-wrap gap-3">
                {['Fantasy', 'Business', 'Self-Help', 'Romance', 'Sci-Fi', 'Thriller'].map(c => (
                  <div key={c} className="px-5 py-2.5 bg-white border border-slate-200 rounded-full text-sm font-medium text-slate-700 shadow-sm">
                    {c}
                  </div>
                ))}
              </div>
            </div>
            <div>
               <h2 className="text-lg font-bold text-slate-900 mb-4">Recent Searches</h2>
               <div className="space-y-3">
                 {['Atomic Habits', 'Sarah J. Maas', 'Dune'].map(s => (
                   <div key={s} className="flex items-center justify-between py-2 border-b border-slate-100 last:border-0">
                     <span className="text-slate-600">{s}</span>
                     <SearchIcon size={16} className="text-slate-300" />
                   </div>
                 ))}
               </div>
            </div>
          </div>
        ) : (
          <div>
            <h2 className="text-sm font-medium text-slate-500 mb-4">Results for "{query}"</h2>
            {/* Search results mock */}
            <div className="space-y-4">
                {[1, 2].map(i => (
                  <div key={i} className="flex gap-4 items-center bg-white p-3 rounded-2xl shadow-sm">
                    <img src="https://images.unsplash.com/photo-1641154748135-8032a61a3f80?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxidXNpbmVzcyUyMGJvb2slMjBjb3ZlcnxlbnwxfHx8fDE3Nzg3NjI5ODF8MA&ixlib=rb-4.1.0&q=80&w=1080" alt="Cover" className="w-16 h-16 rounded-xl object-cover" />
                    <div className="flex-1">
                      <h3 className="font-semibold text-slate-900">Business Masterclass</h3>
                      <p className="text-xs text-slate-500 mb-1">John Doe</p>
                    </div>
                  </div>
                ))}
              </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ---- LIBRARY SCREEN ----
export function Library() {
  const [tab, setTab] = useState('in-progress');
  const navigate = useNavigate();

  return (
    <div className="flex flex-col h-full bg-slate-50">
      <div className="pt-8 px-6 pb-4 bg-white z-10">
        <h1 className="text-2xl font-bold text-slate-900">My Library</h1>
      </div>

      <div className="px-6 py-4 bg-white shadow-sm mb-2">
        <div className="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
          {['Saved', 'In Progress', 'Downloaded', 'Completed'].map(t => {
            const val = t.toLowerCase().replace(' ', '-');
            return (
              <button 
                key={val}
                onClick={() => setTab(val)}
                className={`whitespace-nowrap px-4 py-2 text-sm font-medium rounded-full transition-all ${tab === val ? 'bg-violet-600 text-white' : 'bg-slate-100 text-slate-600'}`}
              >
                {t}
              </button>
            )
          })}
        </div>
      </div>

      <div className="flex-1 px-6 py-4 overflow-y-auto">
         {tab === 'in-progress' && (
           <div className="space-y-4">
             {[1, 2].map(i => (
                <div key={i} onClick={() => navigate(`/player/${i}`)} className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 cursor-pointer">
                  <div className="flex gap-4">
                    <img src="https://images.unsplash.com/photo-1711185901036-f7fd98e50bb1?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxyb21hbmNlJTIwYm9vayUyMGNvdmVyfGVufDF8fHx8MTc3ODc5Mzg5M3ww&ixlib=rb-4.1.0&q=80&w=1080" alt="Cover" className="w-20 h-28 rounded-xl object-cover shadow-sm" />
                    <div className="flex-1 py-1 flex flex-col">
                      <h3 className="font-bold text-slate-900 leading-tight mb-1">Love in the City</h3>
                      <p className="text-xs text-slate-500 mb-auto">Emma Watson</p>
                      
                      <div className="mt-3">
                        <div className="flex justify-between text-xs text-slate-500 mb-1.5 font-medium">
                          <span>Chapter 4 of 12</span>
                          <span>34%</span>
                        </div>
                        <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
                          <div className="bg-violet-600 h-full rounded-full" style={{ width: '34%' }} />
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
             ))}
           </div>
         )}
         {tab !== 'in-progress' && (
           <div className="flex flex-col items-center justify-center py-20 text-center">
             <Bookmark size={48} className="text-slate-300 mb-4" />
             <h3 className="text-lg font-semibold text-slate-700 capitalize">{tab.replace('-', ' ')}</h3>
             <p className="text-slate-500 text-sm mt-2">Items will appear here.</p>
           </div>
         )}
      </div>
    </div>
  );
}

// ---- CHALLENGE SCREEN ----
export function Challenge() {
  return (
    <div className="flex flex-col h-full bg-slate-50">
      <div className="pt-8 px-6 pb-6 bg-violet-600 text-white rounded-b-[40px] shadow-sm">
        <h1 className="text-2xl font-bold mb-6">Challenges</h1>
        
        <div className="bg-white/10 backdrop-blur-md rounded-2xl p-5 border border-white/20">
          <div className="flex justify-between items-center mb-4">
            <div>
              <p className="text-violet-100 text-sm font-medium">Current Goal</p>
              <h2 className="text-xl font-bold">12 / 20 Books</h2>
            </div>
            <div className="w-12 h-12 bg-white rounded-full flex items-center justify-center text-violet-600 font-bold">
              60%
            </div>
          </div>
          <div className="w-full bg-black/20 h-2 rounded-full overflow-hidden">
            <div className="bg-white h-full rounded-full" style={{ width: '60%' }} />
          </div>
        </div>
      </div>

      <div className="flex-1 px-6 py-6 overflow-y-auto">
        <h2 className="text-lg font-bold text-slate-900 mb-4">Active Challenges</h2>
        <div className="space-y-4">
          <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
            <div className="w-12 h-12 bg-orange-100 text-orange-600 rounded-full flex items-center justify-center shrink-0">
              <Trophy size={24} />
            </div>
            <div className="flex-1">
              <h3 className="font-bold text-slate-900">Summer Reading</h3>
              <p className="text-xs text-slate-500">Read 5 fiction books</p>
            </div>
            <span className="text-sm font-bold text-orange-600">3/5</span>
          </div>

          <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center gap-4">
            <div className="w-12 h-12 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center shrink-0">
              <BookOpen size={24} />
            </div>
            <div className="flex-1">
              <h3 className="font-bold text-slate-900">7 Day Streak</h3>
              <p className="text-xs text-slate-500">Listen 30m every day</p>
            </div>
            <span className="text-sm font-bold text-blue-600">Day 4</span>
          </div>
        </div>

        <h2 className="text-lg font-bold text-slate-900 mt-8 mb-4">Achievements</h2>
        <div className="grid grid-cols-3 gap-4">
          {[1, 2, 3, 4, 5, 6].map(i => (
            <div key={i} className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex flex-col items-center justify-center text-center gap-2 grayscale hover:grayscale-0 transition-all cursor-pointer">
              <div className="w-12 h-12 bg-amber-100 text-amber-500 rounded-full flex items-center justify-center">
                <Star size={20} />
              </div>
              <span className="text-[10px] font-bold text-slate-600 leading-tight">Fast<br/>Reader</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ---- PROFILE SCREEN ----
export function Profile() {
  const navigate = useNavigate();

  const sections = [
    { title: 'Account Info', icon: User },
    { title: 'My Books', icon: BookOpen },
    { title: 'My Reviews & Ratings', icon: Star },
    { title: 'Achievements', icon: Trophy },
    { title: 'Subscription (Pro)', icon: Play },
    { title: 'Settings', icon: Settings },
  ];

  return (
    <div className="flex flex-col h-full bg-slate-50">
      <div className="pt-8 px-6 pb-6 bg-white shadow-sm z-10 flex flex-col items-center">
        <div className="w-24 h-24 bg-slate-200 rounded-full mb-4 border-4 border-white shadow-md relative overflow-hidden">
          <img src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9maWxlJTIwcGljdHVyZXxlbnwxfHx8fDE3Nzg4MjYwMjZ8MA&ixlib=rb-4.1.0&q=80&w=1080" className="w-full h-full object-cover" alt="Profile" />
        </div>
        <h1 className="text-xl font-bold text-slate-900">Alex Johnson</h1>
        <p className="text-sm text-slate-500 mb-4">Premium Member</p>
        
        <div className="flex gap-8 text-center w-full justify-center px-4">
          <div>
            <div className="text-xl font-bold text-slate-900">124</div>
            <div className="text-xs text-slate-500">Books</div>
          </div>
          <div className="w-px bg-slate-200"></div>
          <div>
            <div className="text-xl font-bold text-slate-900">45</div>
            <div className="text-xs text-slate-500">Reviews</div>
          </div>
          <div className="w-px bg-slate-200"></div>
          <div>
            <div className="text-xl font-bold text-slate-900">12</div>
            <div className="text-xs text-slate-500">Badges</div>
          </div>
        </div>
      </div>

      <div className="flex-1 px-6 py-6 overflow-y-auto space-y-2">
        {sections.map((sec, idx) => (
          <button key={idx} className="w-full bg-white p-4 rounded-2xl flex items-center justify-between shadow-sm border border-slate-100 active:scale-[0.98] transition-transform">
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 bg-slate-50 rounded-full flex items-center justify-center text-slate-600">
                <sec.icon size={20} />
              </div>
              <span className="font-semibold text-slate-800">{sec.title}</span>
            </div>
            <ChevronRight size={20} className="text-slate-400" />
          </button>
        ))}
        
        <button onClick={() => navigate('/')} className="w-full bg-white p-4 rounded-2xl flex items-center justify-between shadow-sm border border-slate-100 mt-4 active:scale-[0.98] transition-transform">
            <div className="flex items-center gap-4 text-red-600">
              <div className="w-10 h-10 bg-red-50 rounded-full flex items-center justify-center">
                <Settings size={20} />
              </div>
              <span className="font-semibold">Log Out</span>
            </div>
          </button>
      </div>
    </div>
  );
}
